package org.redukti.rayoptics.analysis;

import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.raytr.RayFanType;
import org.redukti.rayoptics.raytr.TraceFanResult;

import java.util.ArrayList;
import java.util.List;

public class RayAberrationResult {

    public List<TraceFanResult> results = new ArrayList<TraceFanResult>();

    public void add(TraceFanResult fan_result){
        results.add(fan_result);
    }


}
