package com.mati.resultados;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

class Utils {
    static String getStringFromUrl(String url) throws IOException {
        InputStream inputStream = new URL(url).openStream();

        Scanner scanner = new Scanner(inputStream, "UTF-8");
        scanner.useDelimiter("\\A");

        return scanner.next();
    }
}
