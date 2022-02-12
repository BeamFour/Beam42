package org.redukti.rayoptics.elem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class Node {
    public String name;
    public Object id;
    public String tag;
    private Node parent;
    private List<Node> children = new ArrayList<>();

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
        if ("root".equals(name) && parent != null) {
            throw new IllegalArgumentException("root cannot have a parent");
        }
        this.name = name;
        this.id = id;
        this.tag = tag;
        this.parent = parent;
        if (parent != null)
            parent.children.add(this);
    }

    /**
     * depth first scan
     */
    public void dfsScan(Consumer<Node> f) {
        f.accept(this);
        for (Node c : children) {
            c.dfsScan(f);
        }
    }

    public List<Node> all() {
        List<Node> result = new ArrayList<>();
        dfsScan((n) -> {
            result.add(n);
        });
        return result;
    }

    /**
     * String representing the node hierarchy
     */
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


    public void set_parent(Node p) {
        if (parent != p) {
            checkLoop(p);
            detach();
            attach(p);
        }
    }

    private void attach(Node p) {
        this.parent = p;
        if (p != null) {
            p.children.add(this);
        }
    }

    private void detach() {
        if (parent != null) {
            parent.children.remove(this);
            parent = null;
        }
    }

    private void checkLoop(Node p) {
        if (p != null) {
            if (children.contains(p)) {
                throw new IllegalArgumentException();
            }
        }
    }

    public Node parent() {
        return this.parent;
    }

    public void set_children(List<Node> newChildren) {
        Node[] todetach = children.toArray(new Node[children.size()]);
        for (Node c : todetach) {
            c.detach();
        }
        for (Node c : newChildren) {
            c.attach(this);
        }
    }

    public List<Node> children() {
        return children;
    }
}
