import SupportingClasses.TheGraphQueryMaker;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class Temp {
    public static void main(String[] args) throws Exception {
        TheGraphQueryMaker theGraphQueryMaker = new TheGraphQueryMaker("https://api.thegraph.com/subgraphs/name/sushiswap/bsc-exchange", System.out);
        theGraphQueryMaker.setGraphQLQuery("""
                {
                    pairs(first: 100) {
                        token0 {
                            id
                        }
                        token1 {
                            id
                        }
                    }
                }""");

        File file = new File("out.txt");
        if (!file.exists()) {
            if (!file.createNewFile()) {
                throw new Exception("Unable to create the file...");
            }
        }
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        PrintStream printStream = new PrintStream(fileOutputStream);

        JSONArray jsonArray = theGraphQueryMaker.sendQuery().getJSONArray("pairs");
        System.out.println("Printing...");
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String token0 = jsonObject.getJSONObject("token0").getString("id");
            String token1 = jsonObject.getJSONObject("token1").getString("id");

            printStream.println("addNewPair BSC " + token0 + " " + token1);
        }
        System.out.println("Printing over....");
        printStream.close();
        fileOutputStream.close();
    }
}
