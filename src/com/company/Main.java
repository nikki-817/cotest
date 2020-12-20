package com.company;

import com.company.GrammerAnalyse.GrammerAnalyse;
import com.company.TokenAnalyse.Token;
import com.company.TokenAnalyse.TokenAnalyse;
import com.company.Util.Pair;

import java.io.IOException;
import java.util.Optional;

public class Main {
    public static void main(String[] args){
        GrammerAnalyse grammerAnalyser = new GrammerAnalyse();
        File file = new File(args[0]);
        InputStreamReader reader;
        BufferedReader br = null;
        try {
            reader = new InputStreamReader(new FileInputStream(file));
            br = new BufferedReader(reader);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            String in = br.readLine();
            while (in != null) {
                System.out.println(in);
                in = br.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(-1);
    }
}
