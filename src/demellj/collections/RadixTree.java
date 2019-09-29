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
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    /**
     * O(n) search value stored in this radix tree. Where n is the number of values stored
     * in this tree.
     *
     * @param value the value to search
     * @return true if value is stored somewhere in the tree
     */
    @Override
    public boolean containsValue(Object value) {
        if (value == null) return false;

        for (final V v : values()) {
            if (value.equals(v))
                return true;
        }

        return false;
    }

    public boolean containsPrefix(String key) {
        return findMatchingPrefixEnd(key, 0).matchEnd == key.length();
    }

    /**
     * O(|text|^2) search for all keys occurring the specified text
     *
     * @param text the text in which find all key occurrences
     * @return an unsorted list of complete key matches
     */
    public ArrayList<KeyMatch> findKeys(String text) {
        final ArrayList<KeyMatch> result = new ArrayList<>();

        if (root.isValueNode()) // account of empty prefix
            result.add(new KeyMatch(root, null, 0, 0));

        final List<KeyMatch> matches = findAllNonemptyPrefixMatches(text);

        for (final KeyMatch match : matches) {
            if (match.node.end == match.matchEnd - match.matchStart &&
                    match.node.isValueNode())
                result.add(match);
        }

        return result;
    }

    @Override
    public V get(Object o) {
        if (!(o instanceof String))
            return null;

        final String key = (String) o;
        final KeyMatch match = findMatchingPrefixEnd(key, 0);

        if (match.matchEnd == key.length() &&
                match.matchEnd == match.node.end &&
                match.node.value != null)
            return match.node.value;

        return null;
    }

    @Override
    public V put(String key, V value) {
        if (value == null || key == null)
            return null;

        final int keyLength = key.length();

        final KeyMatch match = findMatchingPrefixEnd(key, 0);

        if (match.node.end != match.matchEnd) {
            // match ends within but not at end of node substring
            // NOTE: a match cannot end before node substring start, by nature
            // of how findMatchingPrefixEnd(..) works

            match.node.splitAt(match.matchEnd); // this changes match.node.end

            if (match.node.end == keyLength) {
                // key is fully matched
                match.node.value = value;
            } else {
                // key is longer than node substring
                final Node extension = new Node(key, match.matchEnd, keyLength);
                match.node.add(extension);
                extension.value = value;
            }

            size++;
        } else {
            // match is complete (ends at end of this node substring)
            if (match.matchEnd == keyLength) {
                // key is fully matched
                final V presentValue = match.node.value;
                if (presentValue == null)
                    size++;
                match.node.value = value;
                return presentValue;
            } else {
                // key is partially matched (key is longer than the matched prefix)
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
        final KeyMatch match = findMatchingPrefixEnd(key, 0);

        if (match.matchEnd == key.length() &&
                match.matchEnd == match.node.end &&
                match.node.value != null) {
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
        final KeyMatch match = findMatchingPrefixEnd(prefix, 0);

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
        return collectEntries(root);
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

        final KeyMatch match = findMatchingPrefixEnd(prefix, 0);

        if (match.matchEnd == prefix.length())
            return collectEntries(match.node);

        return new HashSet<>();
    }

    private Set<Entry<String, V>> collectEntries(Node start) {
        assert start != null;

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
     *
     * @param key the string against which to find a matching prefix
     * @param startOffset start the search at this index into key
     * @return the prefix match as a PrefixMatch object
     */
    private KeyMatch findMatchingPrefixEnd(String key, int startOffset) {
        assert key != null;
        assert key.length() == 0 || (startOffset >= 0 && startOffset < key.length());

        Node parent = null;
        Node node = root;
        int offset = startOffset;

        final int keyLength = key.length();

        while (true) {
            final int branchLength = node.branchLength();
            final int minLength = Math.min(branchLength, keyLength - offset);

            for (int i = 0; i < minLength; ++i) {
                final int diff = node.ref.charAt(node.start + i) - key.charAt(offset + i);

                if (diff != 0) // partial match
                    return new KeyMatch(node, parent, startOffset, offset + i);
            }

            // NOTE: offset + minLength <= keyLength
            offset += minLength;

            // Might have matched up to somewhere within this node
            if (offset == keyLength || offset < node.end)
                return new KeyMatch(node, parent, startOffset, offset);

            // Need to find if there is a child to continue matching
            final Node child = node.findChildNodeStartsWith(key.charAt(offset));

            if (child == null)
                return new KeyMatch(node, parent, startOffset, offset);

            parent = node;
            node = child;
        }
    }

    /**
     * Finds the location all non-empty prefixes that are contained in text.
     *
     * @param text the text to search for prefixes
     * @return a list of KeyMatches, which is empty when nothing matched.
     */
    private List<KeyMatch> findAllNonemptyPrefixMatches(String text) {
        assert text != null;

        final List<KeyMatch> result = new LinkedList<>();

        for (int offset = 0; offset < text.length(); ++offset) {
            final KeyMatch match = findMatchingPrefixEnd(text, offset);

            if (match.node == root)
                continue;

            result.add(match);
        }

        return result;
    }

    public class KeyMatch {
        private final Node nodeParent;
        private final Node node;

        public final int matchStart; // inclusive
        public final int matchEnd;   // exclusive

        private KeyMatch(Node node, Node nodeParent, int matchStart, int matchEnd) {
            this.node = node;
            this.nodeParent = nodeParent;
            this.matchStart = matchStart;
            this.matchEnd = matchEnd;
        }

        private String getMatchedPrefix() {
            return node.ref.substring(0, matchEnd - matchStart);
        }

        public Entry<String, V> getEntry() {
            return matchEnd - matchStart == node.end ? node : null;
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
         *
         * @param ref   the reference key
         * @param start the start offset in key (inclusive)
         * @param end   the end offset in key (exclusive)
         */
        private Node(String ref, int start, int end) {
            this.ref = ref;
            this.start = start;
            this.end = end;
        }

        private void add(Node child) {
            assert child != null;
            assert !child.ref.isEmpty();
            assert child.start < child.ref.length();

            if (children == null)
                children = new HashMap<>();

            children.put(child.ref.charAt(child.start), child);
        }

        /**
         * Split this Node into two, at the specified index
         *
         * @param index at which to split this node, must be within range or the operation is aborted
         * @return returns the node that contains the rest of the split, while this node remains
         * the head of the split. Will return null if index is not in range.
         */
        private Node splitAt(int index) {
            assert index >= start && index < end;

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
            assert child != null;
            assert child.start < child.ref.length();

            final Character idx = child.ref.charAt(child.start);

            if (!isLeafNode()) {
                final Node actualChild = children.get(idx);

                if (actualChild == child)
                    return children.remove(idx) != null;
            }

            return false;
        }

        /**
         * Get the length of the substring matched by this node
         *
         * @return
         */
        private int branchLength() {
            return end - start;
        }

        private Node findChildNodeStartsWith(char idx) {
            if (isLeafNode())
                return null;

            return children.get(idx);
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
