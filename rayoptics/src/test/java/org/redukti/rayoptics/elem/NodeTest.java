package org.redukti.rayoptics.elem;

import org.junit.jupiter.api.Test;

public class NodeTest {

    @Test
    public void testLeaves() {

        Node udo = new Node("Udo");
        Node marc = new Node("Marc", udo);
        Node lian = new Node("Lian", marc);
        Node loui = new Node("Loui", marc);
        Node lazy = new Node("Lazy", marc);
        System.out.println(udo.leaves());
        //(Node('/Udo/Marc/Lian'), Node('/Udo/Marc/Loui'), Node('/Udo/Marc/Lazy'))
        System.out.println(marc.leaves());
        //(Node('/Udo/Marc/Lian'), Node('/Udo/Marc/Loui'), Node('/Udo/Marc/Lazy'))
    }
}
