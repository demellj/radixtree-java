package demellj.collections;

import java.util.*;

public class RadixTreeTest {
    public static void main(String[] args) {
        final HashMap<String, String> reference = new HashMap<>();

        reference.put("test", "abc");
        reference.put("testing", "123");
        reference.put("tea", "party");
        reference.put("foo", "bar");
        reference.put("", "Zing");

        final RadixTree<String> tree = new RadixTree<>();

        tree.putAll(reference);

        System.out.println(tree.size() == reference.size());
        System.out.println(tree.get("tin") == null);
        System.out.println(tree.get("tea").equals("party"));
        System.out.println(tree.get("te") == null);
        System.out.println(tree.get("test").equals("abc"));
        System.out.println(tree.get("tear") == null);
        System.out.println(tree.get("foo").equals("bar"));
        System.out.println(tree.get("bar") == null);
        System.out.println(tree.get("testing").equals("123"));
        System.out.println(tree.get("testin") == null);
        System.out.println(tree.get("").equals("Zing"));

        Set<String> resultSet = tree.keySet();
        System.out.println(resultSet.size() == tree.size());

        resultSet = tree.keySet("te");
        System.out.println(resultSet.size() == 3);
        for (String key : tree.keySet("te")) {
            System.out.println(key.startsWith("te"));
        }

        resultSet = tree.keySet("tes");
        System.out.println(resultSet.size() == 2);
        for (String key : resultSet) {
            System.out.println(key.startsWith("tes"));
        }

        System.out.println(tree.remove("testin") == null);
        System.out.println(tree.size() == reference.size());

        String val = tree.remove("test");
        System.out.println(val.equals(reference.get("test")));
        System.out.println(tree.get("test") == null);
        System.out.println(tree.size() == reference.size()-1);

        val = tree.remove("testing");
        System.out.println(val.equals(reference.get("testing")));
        System.out.println(tree.get("testing") == null);
        System.out.println(tree.size() == reference.size()-2);

        // put values back in, longer one first
        tree.put("testing", reference.get("testing"));
        tree.put("test", reference.get("test"));
        System.out.println(tree.size() == reference.size());
        System.out.println(tree.get("testing").equals(reference.get("testing")));
        System.out.println(tree.get("test").equals(reference.get("test")));

        tree.removePrefix("test");
        System.out.println(tree.size() == reference.size()-2);

        // put values back in, shorter one first
        tree.put("test", reference.get("test"));
        tree.put("testing", reference.get("testing"));
        System.out.println(tree.size() == reference.size());
        System.out.println(tree.get("testing").equals(reference.get("testing")));
        System.out.println(tree.get("test").equals(reference.get("test")));

        for (Map.Entry<String, String> entry : tree.removePrefix("t")) {
            System.out.println(entry.getKey().startsWith("te"));
            System.out.println(entry.getValue().equals(reference.get(entry.getKey())));
        }
        System.out.println(tree.size() == reference.size()-3);
        System.out.println(tree.get("testing") == null);
        System.out.println(tree.get("tea") == null);
        System.out.println(tree.get("test") == null);

        System.out.println(tree.removePrefix("z").size() == 0);
        System.out.println(tree.remove("yyy") == null);
        System.out.println(tree.size() == reference.size()-3);

        System.out.println(tree.removePrefix("").size() == reference.size()-3);
        System.out.println(tree.isEmpty());

        tree.putAll(reference);
        System.out.println(tree.size() == reference.size());
        System.out.println(tree.remove("").equals(reference.get("")));
        System.out.println(tree.size() == reference.size()-1);
        for (String key : reference.keySet()) {
            if (key.equals("")) continue;
            System.out.println(tree.get(key).equals(reference.get(key)));
        }
        tree.put("", reference.get(""));

        final ArrayList<RadixTree<String>.KeyMatch> matches = tree.findKeys("testing this te cold tested tea");
        System.out.println(matches.size() == 4);
        final HashSet<String> matchedKeys = new HashSet<>();
        for (RadixTree.KeyMatch match : matches) {
            final Map.Entry<String, String> entry = match.getEntry();
            System.out.println(reference.containsKey(entry.getKey()));
            System.out.println(reference.get(entry.getKey()).equals(entry.getValue()));
            matchedKeys.add(entry.getKey());
        }
        System.out.println(matchedKeys.size() == 4); // check for duplicate matches

        System.out.println(tree.containsPrefix(""));
        System.out.println(!tree.containsPrefix("z"));

        // exhaustive prefix and key/value checks
        for (String refKey : reference.keySet()) {
            final int keyLength = refKey.length();
            for (int i = 1; i <= keyLength; ++i) {
                final String key = refKey.substring(0, i);
                if (!reference.containsKey(key)) {
                    System.out.println(!tree.containsKey(key));
                    System.out.println(tree.get(key) == null);
                    final int size = tree.size();
                    System.out.println(tree.remove(key) == null);
                    System.out.println(tree.size() == size);
                    final Set<Map.Entry<String, String>> values = tree.removePrefix(key);
                    System.out.println(tree.size() == size - values.size());
                    for (Map.Entry<String, String> entry : values) {
                        System.out.println(!tree.containsKey(entry.getKey()));
                        System.out.println(tree.get(entry.getKey()) == null);
                        tree.put(entry.getKey(), entry.getValue());
                    }
                    System.out.println(tree.size() == size);
                } else {
                    System.out.println(tree.get(key).equals(reference.get(key)));
                    System.out.println(tree.containsPrefix(key));
                    System.out.println(tree.containsValue(reference.get(key)));
                    final int size = tree.size();
                    final String value = tree.remove(key);
                    if (value != null) {
                        System.out.println(tree.size() == size-1);
                        tree.put(key, value);
                        System.out.println(true);
                    } else {
                        System.out.println(false);
                    }
                    System.out.println(tree.size() == size);
                }
                System.out.println(!tree.containsKey(key+"!"));
                System.out.println(tree.containsPrefix(key));
                System.out.println(!tree.containsPrefix(key + "!"));
            }
        }
    }
}
