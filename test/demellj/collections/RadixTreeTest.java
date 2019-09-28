package demellj.collections;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
        System.out.println(tree.get("").equals("Zing"));

        Set<String> resultSet = tree.keySet();
        System.out.println(resultSet.size() == tree.size());
        for (String key : reference.keySet()) {
            System.out.println(tree.get(key).equals(reference.get(key)));
            System.out.println(tree.containsKey(key));
            System.out.println(tree.containsValue(reference.get(key)));
        }

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

        String val = tree.remove("test");
        System.out.println(val.equals(reference.get("test")));
        System.out.println(tree.get("test") == null);
        System.out.println(tree.size() == reference.size()-1);

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

        System.out.println(tree.containsPrefix(""));
        for (String key : reference.keySet()) {
            final int keyLength = key.length();
            for (int i = 1; i < keyLength; ++i) {
                System.out.println(tree.containsPrefix(key.substring(0, keyLength)));
                System.out.println(!tree.containsPrefix(key.substring(0, keyLength) + "!"));
            }
        }
        System.out.println(!tree.containsPrefix("z"));
    }
}
