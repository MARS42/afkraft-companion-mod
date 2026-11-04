package com.mars4two.ikang.core.snapshots.model;

import java.util.List;

public interface RawObject {

    Object[] payloadValues();

    default Object[] toPayload() {
        Object[] values = this.payloadValues();
        Object[] payload = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            payload[i] = processValue(values[i]);
        }
        return payload;
    }

    private static Object processValue(Object v) {
        if (v == null) {
            return "";
        }
        if (v instanceof RawObject rawObject) {
            return rawObject.toPayload();
        }
        if (v instanceof List<?> list) {
            Object[] array = new Object[list.size()];
            for (int j = 0; j < list.size(); j++) {
                array[j] = processValue(list.get(j));
            }
            return array;
        }
        if (v instanceof Object[] arr) {
            Object[] array = new Object[arr.length];
            for (int j = 0; j < arr.length; j++) {
                array[j] = processValue(arr[j]);
            }
            return array;
        }
        return v;
    }
}
