/*
 *     This file is part of the LibreSpeed speedtest library.
 *
 *     The LibreSpeed speedtest library is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Foobar is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fdossena.speedtest.core;

import java.io.BufferedReader;
import java.util.HashMap;

import com.fdossena.speedtest.core.config.SpeedtestConfig;
import com.fdossena.speedtest.core.Connection;
import com.fdossena.speedtest.core.Utils;

abstract class GetIP extends Thread{
    private Connection c;
    private String path;
    private boolean isp;
    private String distance;
    GetIP(Connection c, String path, boolean isp, String distance){
        this.c=c;
        this.path=path;
        this.isp=isp;
        if(!(distance==null||distance.equals(SpeedtestConfig.DISTANCE_KM)||distance.equals(SpeedtestConfig.DISTANCE_MILES))) throw new IllegalArgumentException("Distance must be null, mi or km");
        this.distance=distance;
        start();
    }

    public void run(){
        try{
            String s=path;
            if(isp){
                s+= Utils.url_sep(s)+"isp=true";
                if(!distance.equals(SpeedtestConfig.DISTANCE_NO)){
                    s+=Utils.url_sep(s)+"distance="+distance;
                }
            }
            c.GET(s,true);
            HashMap<String,String> h=c.parseResponseHeaders();
            BufferedReader br=new BufferedReader(c.getInputStreamReader());
            if(h.get("content-length")!=null){
                //standard encoding
                char[] buf=new char[Integer.parseInt(h.get("content-length"))];
                br.read(buf);
                String data=new String(buf);
                onDataReceived(data);
            }else{
                //chunked encoding hack. TODO: improve this garbage with proper chunked support
                c.readLineUnbuffered(); //ignore first line
                String data=c.readLineUnbuffered(); //actual info we want
                c.readLineUnbuffered(); //ignore last line (0)
                onDataReceived(data);
            }

            c.close();
        }catch(Throwable t){
            try{c.close();}catch(Throwable t1){}
            onError(t.toString());
        }
    }

    abstract void onDataReceived(String data);
    abstract void onError(String err);
}
