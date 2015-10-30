package com.keepcloseapp.keepclose;

import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunction;
import com.google.gson.JsonObject;


import org.json.JSONObject;

public interface LoginInterface {
    @LambdaFunction
    JsonObject LambdaKCLogin(LoginEventRequest lambdaEvent);

}
