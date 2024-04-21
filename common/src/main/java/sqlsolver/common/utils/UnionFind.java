package sqlsolver.common.utils;

import java.util.HashMap;
import java.util.Map;

public class UnionFind<T> {
    private Map<T, T> parent;
    private Map<T, Integer> rank;

    public UnionFind() {
        parent = new HashMap<>();
        rank = new HashMap<>();
    }

    public void makeSet(T x) {
        if (parent.get(x) != null){
            return;
        }
        parent.put(x, x);
        rank.put(x, 0);
    }

    public T find(T x) {
        if (x != parent.get(x)) {
            parent.put(x, find(parent.get(x)));
        }
        return parent.get(x);
    }

    public void union(T x, T y) {
        T rootX = find(x);
        T rootY = find(y);

        if (rootX == rootY) {
            return;
        }

        if (rank.get(rootX) < rank.get(rootY)) {
            parent.put(rootX, rootY);
        } else if (rank.get(rootX) > rank.get(rootY)) {
            parent.put(rootY, rootX);
        } else {
            parent.put(rootY, rootX);
            rank.put(rootX, rank.get(rootX) + 1);
        }
    }
}