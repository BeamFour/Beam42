package org.redukti.rayoptics.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ListsTest {

    @Test
    public void testSlicing() {
        List<Integer> myList = List.of(0,1,2,3,4,5,6,7);
        Assertions.assertEquals(Lists.slice(myList, null,null,-1), List.of(7, 6, 5, 4, 3, 2, 1, 0));
        Assertions.assertEquals(Lists.upto(myList, 3), List.of(0, 1, 2));
        Assertions.assertEquals(Lists.from(myList, 3), List.of(3, 4, 5, 6, 7));
        Assertions.assertEquals(Lists.slice(myList, null,null,-2), List.of(7, 5, 3, 1));
        Assertions.assertEquals(Lists.slice(myList, null,null,2), List.of(0, 2, 4, 6));
        Assertions.assertEquals(Lists.slice(myList, null,-1,2), List.of(0, 2, 4, 6));
        Assertions.assertEquals(Lists.slice(myList, null,5,null), List.of(0, 1, 2, 3, 4));
        Assertions.assertEquals(Lists.slice(myList, null,-1,null), List.of(0, 1, 2, 3, 4, 5, 6));
        Assertions.assertEquals(Lists.slice(myList, 4,null, null), List.of(4, 5, 6, 7));
        Assertions.assertEquals(Lists.slice(myList, -3,null, null), List.of(5, 6, 7));
        Assertions.assertEquals(Lists.slice(myList, 2,5, null), List.of(2, 3, 4));
        Assertions.assertEquals(Lists.slice(myList, 2,-1, null), List.of(2, 3, 4, 5, 6));
        Assertions.assertEquals(Lists.slice(myList, -3,-1, null), List.of(5, 6));
        Assertions.assertEquals(Lists.slice(myList, null,20, null), List.of(0, 1, 2, 3, 4, 5, 6, 7));
        Assertions.assertEquals(Lists.slice(myList, -20,null, null), List.of(0, 1, 2, 3, 4, 5, 6, 7));
    }
}
