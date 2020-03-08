package com.lightel.opticalfiber;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class RestApiManager {

    private static RestApiManager mInstance = new RestApiManager();
    private APIService mAPIService;
    private final static String CGI_URL = "http://192.168.1.1/";

    private RestApiManager() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(CGI_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mAPIService = retrofit.create(APIService.class);
    }

    public static RestApiManager getInstance() {
        return mInstance;
    }

    void getModel(Callback<JsonObject> callback) {
        Call<JsonObject> call = mAPIService.getModel();
        call.enqueue(callback);
    }

    void getDefault(Callback<JsonObject> callback) {
        Call<JsonObject> call = mAPIService.getDefault();
        call.enqueue(callback);
    }

    void getFrame(Callback<JsonObject> callback) {
        Call<JsonObject> call = mAPIService.getFrame();
        call.enqueue(callback);
    }

    void setBrightness(Callback<JsonObject> callback, String brightness) {
        Call<JsonObject> call = mAPIService.setBrightness(brightness);
        call.enqueue(callback);
    }

    void setContrast(Callback<JsonObject> callback, String contrast) {
        Call<JsonObject> call = mAPIService.setContrast(contrast);
        call.enqueue(callback);
    }

    void setSharpness(Callback<JsonObject> callback, String sharpness) {
        Call<JsonObject> call = mAPIService.setSharpness(sharpness);
        call.enqueue(callback);
    }

    void setGain(Callback<JsonObject> callback, String gain) {
        Call<JsonObject> call = mAPIService.setGain(gain);
        call.enqueue(callback);
    }

    void setAutoExposure(Callback<JsonObject> callback, String autoExposure) {
        Call<JsonObject> call = mAPIService.setAutoExposure(autoExposure);
        call.enqueue(callback);
    }

    void setExposureTime(Callback<JsonObject> callback, String exposureTime) {
        Call<JsonObject> call = mAPIService.setExposureTime(exposureTime);
        call.enqueue(callback);
    }

    void setRes(Callback<JsonObject> callback) {
        Call<JsonObject> call = mAPIService.setRes();
        call.enqueue(callback);
    }

    void getCapBtnState(Callback<JsonObject> callback) {
        Call<JsonObject> call = mAPIService.getCapBtnState();
        call.enqueue(callback);
    }
    void getBatteryState(Callback<Battery> callback) {
        Call<Battery> call = mAPIService.getBatteryState();
        call.enqueue(callback);
    }

    public interface APIService {
        @GET("do/getmodel")
        Call<JsonObject> getModel();

        @GET("do/getiq")
        Call<JsonObject> getDefault();

        @GET("do/getframe")
        Call<JsonObject> getFrame();

        @GET("do/setiq")
        Call<JsonObject> setBrightness(@Query("B") String brightness);

        @GET("do/setiq")
        Call<JsonObject> setContrast(@Query("C") String contrast);

        @GET("do/setiq")
        Call<JsonObject> setSharpness(@Query("S") String sharpness);

        @GET("do/setiq")
        Call<JsonObject> setGain(@Query("G") String gain);

        @GET("do/setiq")
        Call<JsonObject> setAutoExposure(@Query("A") String autoExposure);

        @GET("do/setiq")
        Call<JsonObject> setExposureTime(@Query("E") String exposureTime);

        @GET("do/setres?W=1280&H=480")
        Call<JsonObject> setRes();

        @GET("do/getcap")
        Call<JsonObject> getCapBtnState();

        @GET("do/getbattery")
        Call<Battery> getBatteryState();
    }


    public static class Battery {
        String Status;
        int Percent;

        public Battery(String Status, int Percent) {
            this.Status = Status;
            this.Percent = Percent;
        }
    }
}