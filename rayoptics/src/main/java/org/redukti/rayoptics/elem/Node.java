package org.redukti.rayoptics.elem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class Node {
    public String name;
    public Object id;
    public String tag;
    Node parent;
    public List<Node> children = new ArrayList<>();

    public Node(String name, Object id, String tag) {
        this(name, id, tag, null);
    }

    public Node(String name) {
        this(name, null, null, null);
    }

    public Node(String name, Node parent) {
        this(name, null, null, parent);
    }

    public Node(String name, Object id, String tag, Node parent) {
        this.name = name;
        this.id = id;
        this.tag = tag;
        this.parent = parent;
        if (parent != null)
            parent.children.add(this);
    }

    public void dfsScan(Consumer<Node> f) {
        f.accept(this);
        for (Node c : children) {
            c.dfsScan(f);
        }
    }

    public String path() {
        StringBuilder sb = new StringBuilder();
        if (!is_root())
            sb.append(parent.path());
        sb.append('/');
        sb.append(name);
        return sb.toString();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Node(");
        sb.append(path());
        sb.append(",id=");
        sb.append(Objects.toString(id));
        sb.append(",tag=");
        sb.append(tag);
        sb.append(")");
        return sb.toString();
    }

    public boolean is_leaf() {
        return children.isEmpty();
    }

    public boolean is_root() {
        return parent == null;
    }

    public List<Node> leaves() {
        List<Node> result = new ArrayList<>();
        dfsScan((n) -> {
            if (n.is_leaf())
                result.add(n);
        });
        return result;
    }
}
