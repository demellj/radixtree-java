package demellj.collections;

import java.util.*;

public class RadixTree<V> implements Map<String, V> {
    private Node root = null;
    private int size = 0;

    public RadixTree() {
        this.clear();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size <= 0;
    }

    @Override
    public boolean containsKey(Object o) {
        return get(0) != null;
    }

    @Override
    public boolean containsValue(Object v) {
        if (v == null) return false;

        for (final V value : values()) {
            if (value.equals(v))
                return true;
        }

        return false;
    }

    @Override
    public V get(Object o) {
        if (!(o instanceof String))
            return null;

        final String key = (String) o;
        final PrefixMatch match = findMatchingPrefixEnd(key);

        if (match.matchEnd == key.length() && match.node.value != null)
            return match.node.value;

        return null;
    }

    @Override
    public V put(String key, V value) {
        final int keyLength = key.length();

        final PrefixMatch match = findMatchingPrefixEnd(key);

        // key is fully matched
        if (match.matchEnd == keyLength) {
            // key prefix match ends at end of this node
            if (match.node.end == match.matchEnd) {
                final V presentValue = match.node.value;
                if (presentValue == null)
                    size++;
                match.node.value = value;
                return presentValue;
            } else {
                // key prefix match ends within node
                // NOTE: a key prefix cannot end before node start, by nature
                // of how findMatchingPrefixEnd(..) works
                match.node.splitAt(match.matchEnd);

                final Node extension = new Node(key, match.matchEnd, keyLength);
                match.node.add(extension);
                extension.value = value;
                size++;
            }
        } else { // key is partially matched
            // key partial match ends at end of node
            if (match.node.end == match.matchEnd) {
                final Node extension = new Node(key, match.matchEnd, keyLength);
                match.node.add(extension);
                extension.value = value;
                size++;
            } else {
                // key partial match ends within node
                // NOTE: a key prefix cannot end before node start, by nature
                // of how findMatchingPrefixEnd(..) works
                match.node.splitAt(match.matchEnd);

                final Node extension = new Node(key, match.matchEnd, keyLength);
                match.node.add(extension);
                extension.value = value;
                size++;
            }
        }
        return null;
    }

    @Override
    public V remove(Object o) {
        if (!(o instanceof String))
            return null;

        final String key = (String) o;
        final PrefixMatch match = findMatchingPrefixEnd(key);

        if (match.matchEnd == key.length() && match.node.value != null) {
            final V value = match.node.value;
            match.node.value = null;

            if (match.node != root && match.node.isLeafNode())
                match.nodeParent.removeChild(match.node);
            else if (match.nodeParent != null && match.nodeParent != root)
                match.nodeParent.tryMerge();

            size--;
            return value;
        }

        return null;
    }

    public Set<Entry<String, V>> removePrefix(String prefix) {
        final PrefixMatch match = findMatchingPrefixEnd(prefix);

        Set<Entry<String, V>> result = null;

        if (match.matchEnd == prefix.length()) {
            if (match.node == root) {
                result = collectEntries(match.node);
                clear();
            } else {
                if (match.nodeParent.removeChild(match.node)) {
                    result = collectEntries(match.node);
                    size -= result.size();
                }
            }
        }

        return result == null ? new HashSet<>() : result;
    }

    @Override
    public void putAll(Map<? extends String, ? extends V> map) {
        for (Entry<? extends String, ? extends V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        root = new Node("", 0, 0);
        size = 0;
    }

    @Override
    public Set<String> keySet() {
        final HashSet<String> keys = new HashSet<>();
        for (final Entry<String, V> entry : entrySet())
            keys.add(entry.getKey());
        return keys;
    }

    @Override
    public Collection<V> values() {
        final ArrayList<V> values = new ArrayList<>();
        for (final Entry<String, V> entry : entrySet())
            values.add(entry.getValue());
        return values;
    }

    @Override
    public Set<Entry<String, V>> entrySet() {
        return entrySet("");
    }

    public Set<String> keySet(String prefix) {
        final HashSet<String> keys = new HashSet<>();
        for (final Entry<String, V> entry : entrySet(prefix))
            keys.add(entry.getKey());
        return keys;
    }

    public Collection<V> values(String prefix) {
        final ArrayList<V> values = new ArrayList<>();
        for (final Entry<String, V> entry : entrySet(prefix))
            values.add(entry.getValue());
        return values;
    }

    public Set<Entry<String, V>> entrySet(String prefix) {
        assert prefix != null;

        final PrefixMatch match = findMatchingPrefixEnd(prefix);

        if (match.matchEnd == prefix.length())
            return collectEntries(match.node);

        return new HashSet<>();
    }

    private Set<Entry<String, V>> collectEntries(Node start) {
        final HashSet<Entry<String, V>> result = new HashSet<>();

        final Queue<Node> queue = new LinkedList<>();
        queue.offer(start);

        while (!queue.isEmpty()) {
            final Node node = queue.poll();

            if (node.value != null)
                result.add(node);

            if (node.children != null) {
                for (final Node child : node.children.values())
                    queue.offer(child);
            }
        }

        return result;
    }

    /**
     * Returns the last node that matched some prefix of the given key.
     * If nothing matches, it returns the root whose prefix is the empty
     * string "", that matches all substrings.
     * @param key the string against which to find a matching prefix
     * @return the prefix match as a PrefixMatch object
     */
    private PrefixMatch findMatchingPrefixEnd(String key) {
        Node parent = null;
        Node node = root;

        final int keyLength = key.length();
        int offset = 0;

        while (true) {
            final int branchLength = node.branchLength();
            final int minLength = Math.min(branchLength, keyLength - offset);

            for (int i = 0; i < minLength; ++i) {
                final int diff = node.ref.charAt(node.start + i) - key.charAt(offset + i);

                if (diff != 0) // partial match
                    return new PrefixMatch(offset + i, node, parent);
            }

            // NOTE: offset + minLength <= keyLength
            offset += minLength;

            // Might have matched up to somewhere within this node
            if (offset == keyLength || offset < node.end)
                return new PrefixMatch(offset, node, parent);

            // Need to find if there is a child to continue matching
            final Node child = node.findChildNodeStartsWith(key, offset);

            if (child == null)
                return new PrefixMatch(offset, node, parent);

            parent = node;
            node = child;
        }
    }

    private class PrefixMatch {
        final int matchEnd; // exclusive
        final Node node;
        final Node nodeParent;

        /**
         * Construct a PrefixMatch object.
         * @param matchEnd the end index of matching prefix in key (exclusive)
         * @param node the last node that contains a matched prefix
         * @param nodeParent the parent of `node`
         */
        private PrefixMatch(int matchEnd, Node node, Node nodeParent) {
            this.matchEnd = matchEnd;
            this.node = node;
            this.nodeParent = nodeParent;
        }
    }

    private class Node implements Entry<String, V> {
        private String ref;
        private int start; //inclusive
        private int end;   //exclusive

        private HashMap<Character, Node> children = null;
        private V value;

        /**
         * Construct a Node object.
         * @param ref the reference key
         * @param start the start offset in key (inclusive)
         * @param end the end offset in key (exclusive)
         */
        private Node(String ref, int start, int end) {
            this.ref = ref;
            this.start = start;
            this.end = end;
        }

        private void add(Node node) {
            if (children == null)
                children = new HashMap<>();

            if (!node.ref.isEmpty())
                children.put(node.ref.charAt(node.start), node);
        }

        /**
         * Split this Node into two, at the specified index
         * @param index at which to split this node, must be within range or the operation is aborted
         * @return returns the node that contains the rest of the split, while this node remains
         * the head of the split. Will return null if index is not in range.
         */
        private Node splitAt(int index) {
            if (index < start || index >= end)
                return null;
            final Node rest = new Node(ref, index, end);
            rest.children = children;
            rest.value = value;
            children = null;
            value = null;
            end = index;
            add(rest);
            return rest;
        }

        /**
         * Attempt to merge an only-child with this node
         */
        private void tryMerge() {
            if (value == null && children != null && children.size() == 1) {
                Node onlyChild = null;
                for (Character key : children.keySet())
                    onlyChild = children.get(key);
                ref = onlyChild.ref; // child may have a ref that is longer
                end = onlyChild.end;
                children = null;
            }
        }

        private boolean isLeafNode() {
            return children == null;
        }

        private boolean isValueNode() {
            return value != null;
        }

        private boolean removeChild(Node child) {
            final Character idx = child.ref.charAt(child.start);

            if (!isLeafNode()) {
                final Node actualChild = children.get(idx);

                if (actualChild == child) {
                    children.remove(idx);
                    return true;
                }
            }

            return false;
        }

        /**
         * Get the length of the substring matched by this node
         * @return
         */
        private int branchLength() {
            return end - start;
        }

        private Node findChildNodeStartsWith(String ref, int offset) {
            if (isLeafNode())
                return null;

            final Node node = children.get(ref.charAt(offset));

            return node;
        }

        @Override
        public String getKey() {
            return ref.substring(0, end);
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V v) {
            final V previousValue = value;
            value = v;
            return previousValue;
        }
    }
}
