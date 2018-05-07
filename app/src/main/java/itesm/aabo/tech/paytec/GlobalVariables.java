package itesm.aabo.tech.paytec;

import java.util.HashMap;

public class GlobalVariables {
    private static HashMap<String,Integer> valores = new HashMap<>();
    private static String cart;

    public static String getCart() {
        return cart;
    }

    public static void setCart(String cart) {
        GlobalVariables.cart = cart;
    }

    public static Integer hashValue(String id){
        if(valores.containsKey(id)){
            return Integer.parseInt(String.valueOf(valores.get(id)));
        }

        return 0;
    }

    public static void addValue(String id, int value){
        if(valores.containsKey(id)) {
            Integer val = Integer.parseInt(String.valueOf(valores.get(id)));
            valores.put(id,(val+value));
        }else{
            valores.put(id, value);
        }
    }

    public static boolean subtractValue(String id, int value){
        if(valores.containsKey(id)) {
            Integer val = Integer.parseInt(String.valueOf(valores.get(id)));
            if(val-value < 0){
                return false;
            }else{
                valores.put(id,(val-value));
                return true;
            }
        }else{
            return false;
        }
    }
}