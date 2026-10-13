package com.igot.cios.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiRespParamTest {

    @Test
    void noArgConstructor_leavesFieldsNull() {
        ApiRespParam param = new ApiRespParam();
        assertNull(param.getResMsgId());
        assertNull(param.getMsgId());
    }

    @Test
    void idConstructor_setsResMsgIdAndMsgId() {
        ApiRespParam param = new ApiRespParam("abc-123");
        assertEquals("abc-123", param.getResMsgId());
        assertEquals("abc-123", param.getMsgId());
    }

    @Test
    void settersAndGetters_roundTrip() {
        ApiRespParam param = new ApiRespParam();
        param.setResMsgId("res-1");
        param.setMsgId("msg-1");
        param.setErr("err-1");
        param.setStatus("FAILED");
        param.setErrMsg("something went wrong");

        assertEquals("res-1", param.getResMsgId());
        assertEquals("msg-1", param.getMsgId());
        assertEquals("err-1", param.getErr());
        assertEquals("FAILED", param.getStatus());
        assertEquals("something went wrong", param.getErrMsg());
    }
}
