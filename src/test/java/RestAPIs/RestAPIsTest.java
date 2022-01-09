package RestAPIs;

import RestAPIs.RestAPIs;
import java.net.URL;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RestAPIsTest {

    @Test
    void doGetTest(){
        String url = "https://www.bilibili.com";
        RestAPIs restAPIs = new RestAPIs();
        String result = restAPIs.doGet(url);
        System.out.printf("result: %s\n", result);
        assertEquals("200", result);
    }
}
