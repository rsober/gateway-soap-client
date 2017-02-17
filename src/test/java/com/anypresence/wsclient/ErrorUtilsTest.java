package com.anypresence.wsclient;


import com.anypresence.wsclient.utils.ErrorHandlingUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class ErrorUtilsTest {
    @Test
    public void checkError() {
        String json = ErrorHandlingUtils.exToJson(new Exception("error"));

        Assert.assertTrue(!json.isEmpty());
    }
}
