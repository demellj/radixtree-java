package demellj.collections;

import java.util.HashMap;
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
        for (String key : tree.keySet()) {
            System.out.println(tree.get(key) == reference.get(key));
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
    }
}
