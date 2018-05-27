package com.oulu.lock.utils;

import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Random;

/**
 * Created by liao on 2018/4/10.
 */

public class HttpUtils {

        /*public static void httpPost(String url, HashMap<String,String> maps, Handler handler,int flag){
            HttpPost request=new HttpPost(url);
            List<NameValuePair> params=new ArrayList<NameValuePair>();
            Iterator<String> requestHeder=maps.keySet().iterator();
            while (requestHeder.hasNext()){
                String key=requestHeder.next();
                String value=maps.get(key);
                Log.i("liao","key:"+key);
                Log.i("liao","value:"+value);
                params.add(new BasicNameValuePair(key,value));
            }
            try {
                HttpEntity httpEntity=new UrlEncodedFormEntity(params,"utf-8");
                request.setEntity(httpEntity);
                request.addHeader("content-type", "application/x-www-form-urlencoded");
                //request.addHeader("content-type", "application/json");
                HttpClient httpClient = new DefaultHttpClient();
                HttpResponse response=httpClient.execute(request);
                if(response.getStatusLine().getStatusCode()== HttpStatus.SC_OK) {
                    String result = EntityUtils.toString(response.getEntity());
                    JSONObject jsonObject=new JSONObject(result);
                    Log.i("liao",jsonObject.getString("success"));
                    Log.i("liao",jsonObject.getString("msg"));
                    Message m=new Message();
                    if(flag==1) {
                        m.what = 1;
                        if(jsonObject!=null) {
                            m.getData().putString("result", jsonObject.getString("success"));
                            Log.i("liao","reee="+jsonObject.getString("success"));
                        }else{
                            m.getData().putString("result", "未知错误");
                        }
                    }else if(flag==2){
                        m.what = 2;
                        if(jsonObject!=null) {
                            Log.i("liao","reee="+jsonObject.getString("success"));
                            m.getData().putString("result", jsonObject.getString("success"));
                        }else{
                            m.getData().putString("result", "未知错误");
                        }
                    }
                    handler.sendMessage(m);
                    return;
                }
                Log.i("liao","code:"+response.getStatusLine().getStatusCode());
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("liao",e.toString());
            }
            Message m=new Message();
            m.what=-1;
            handler.sendMessage(m);
        }*/
        public static String httpPost(String url, JSONObject values) throws JSONException {
            HttpPost request = new HttpPost(url);
            String result=null;
            try {
                request.setEntity(new StringEntity(values.toString(), HTTP.UTF_8));
                //request.addHeader("content-type", "application/x-www-form-urlencoded");
                request.addHeader("content-type", "application/json");
                HttpClient httpClient = new DefaultHttpClient();
                HttpResponse response = httpClient.execute(request);
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    result= EntityUtils.toString(response.getEntity(), "utf-8");

                    Log.i("liao", result);

                }
                Log.i("liao", "code:" + response.getStatusLine().getStatusCode());
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("liao", e.toString());
                return "error";
            } finally {
                Log.i("liao", "123");
                return result;
            }
        }
        public static void httpGet(String url){
            HttpGet httpRequest=new HttpGet(url);

            HttpClient httpClient=new DefaultHttpClient();
            HttpResponse response= null;
            try {
                response = httpClient.execute(httpRequest);
                if(response.getStatusLine().getStatusCode()== HttpStatus.SC_OK) {
                    String result = EntityUtils.toString(response.getEntity(),"utf-8");
                    Log.i("liao",result);
                }
                Log.i("liao", "code11:" + response.getStatusLine().getStatusCode());
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                Log.i("liao", "123111");
            }

        }
        public static String getRandomCode(){
            Random random = new Random();
            String result="";
            for (int i=0;i<6;i++)
            {
                result+=random.nextInt(10);
            }
            return  result;
        }
    }

