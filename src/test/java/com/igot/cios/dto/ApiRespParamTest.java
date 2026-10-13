package com.igot.cios.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiRespParamTest {

    @Test
    void settersAndGetters_roundTrip() {
        ApiRespParam param = new ApiRespParam();
        param.setResmsgid("res-1");
        param.setMsgid("msg-1");
        param.setErr("err-1");
        param.setStatus("FAILED");
        param.setErrmsg("something went wrong");

        assertEquals("res-1", param.getResmsgid());
        assertEquals("msg-1", param.getMsgid());
        assertEquals("err-1", param.getErr());
        assertEquals("FAILED", param.getStatus());
        assertEquals("something went wrong", param.getErrmsg());
    }

    @Test
    void stringConstructor_leavesFieldsUnset() {
        ApiRespParam param = new ApiRespParam("ignored");

        assertNull(param.getResmsgid());
        assertNull(param.getMsgid());
    }
}
