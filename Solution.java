import java.io.*;
import java.util.*;
import java.text.*;
import java.math.*;
import java.util.regex.*;
import java.net.*;

import com.google.gson.*;
/**
*@author Parteek Dheri (dherip [AT] sheridancollege [DOT] ca)
*/
public class Solution {
    public static void main(String args[] ) throws Exception {
        /* Enter your code here. Read input from STDIN. Print output to STDOUT */
        
        //All of the business logic is in `Cart` class
        Input input = Reader.readSTD_IN();
        Cart myCart = new Cart(input);
        myCart.generateResult();      
    }
}

/* ---------------------------------------------------------------------------------------------*/
class API_Page {
    private Product[] products;
    private Pagination pagination;
    public API_Page(){
    }
    public Product[] getProducts(){
        return products;
    }
}

class Cart {
    private ArrayList<Product> products = new ArrayList(); 
    private String API_URL = "";
    private Input input;
    private Result result = new Result();
    public Cart(Input input){
        this.input = input;
        API_URL = Constants.API_URL +"?id=" + input.getId() ;
        createCart();
    }
    
    private void createCart(){    
        //boolean flag will be turned to false if product array returned is empty
        boolean keepGoing = true; 
        int pageNumber = 1;
        while(keepGoing){ 
            String current_page = "";
            String completeURL = this.API_URL + "&page="+pageNumber;
            try{
                current_page = Reader.readURL(completeURL);
            }catch(Exception e){
                // will log the exception in loger ;-) 
            }
            // System.out.println("page number --> " + pageNumber + " data: "+current_page);
            
            Gson gson = new Gson();
            API_Page api_Page = gson.fromJson(current_page, API_Page.class);
            products.addAll(Arrays.asList(api_Page.getProducts()));// add products on this page to the cart
            pageNumber++; //read next page
            if(api_Page.getProducts().length == 0){ //read till current page have zero products, 
                //no need of division and modulus [on total in pagination] to calculate number of passes, 
                //also this way even if total in pagination is wrong, we can prevent any mistake. 
                //But on cost of one extra querry to api. (keeping in mind that internal call to cart API must not be resource extensive)
                // can switch to division and modulus [on total in pagination] aproach to calculate number of passes is this one is resource extensive. :)
                keepGoing = false; //no need for next querry to API
            }
        }
    }
        

    public Result generateResult (){
        Result result = new Result();
        ResultGenerator rg = new ResultGenerator(products, input);
        try{
            result = rg.calculateTotals();
        }
        catch(NegativeTotalAfterDiscountException ntade){
         //handle or throw up the stack
        }
        
        rg.printResult(); // for evaluation ;)
        return result; // send to front end :*
    }
    

}

//put this in util package please
class ResultGenerator{
    ArrayList<Product> products;
    Input input;
    Result result = new Result();
    Double total = 0.0;
    Double totalWithDiscount = 0.0;
    public ResultGenerator(ArrayList<Product> products, Input input){
        this.products = products;
        this.input = input;
    }
    
    /*
    *I will be assuming that if `discount_type` is `cart`, the key to enforce discount will be `cart_value`
    *and if the `discount_type` is `product`, the key will be either `product_value` or `collection`
    */
    public Result calculateTotals() throws   NegativeTotalAfterDiscountException{
         Double discount_value  =  Double.parseDouble(input.getDiscount_value());
        
        //case 1 when cart_value is given
        if(input.getCart_value() != null){
            Double cart_value =  Double.parseDouble(input.getCart_value());
            for(Product product : products){
                total += Double.parseDouble(product.getPrice());
            }
            totalWithDiscount = total >= cart_value?  priceAfterDiscount(total , discount_value)  : total;
        }
        
        //case 2 when product_value is given
        if(input.getProduct_value() != null){
            Double product_value =  Double.parseDouble(input.getProduct_value());
            for(Product product : products){
                Double productPrice =  Double.parseDouble(product.getPrice());
                total += productPrice;
                totalWithDiscount += productPrice >= product_value ? priceAfterDiscount(productPrice , discount_value)  : productPrice;
            }
           
        }
        
        //case 3 when collection is given
        if(input.getCollection() != null){
            String collection =  input.getCollection();
            for(Product product : products){
                Double productPrice =  Double.parseDouble(product.getPrice());
                total += productPrice;
                if ( product.getCollection() != null){
                    totalWithDiscount += (product.getCollection().equals(collection) ? priceAfterDiscount(productPrice , discount_value)  : productPrice);
                }else{
                    totalWithDiscount += productPrice;
                }
            }
           
        }
        
        /*-----common logic for each case--------*/

        if (totalWithDiscount < 0){
            throw new NegativeTotalAfterDiscountException("you should cancel this transaction and manually investigate the case");
        } 
        
        result.setTotal_amount(total);
        result.setTotal_after_discount(totalWithDiscount);
        this.result = result;
        return result;
    }
    
    public Double priceAfterDiscount(Double price, Double discount){
        if (price > discount){
            return price - discount;
        }
        return 0.0;
    }
    
    public void printResult (){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(result);
        System.out.println(json);
        
    }
}
    
class Product{
    private String name;
    private String price;
    private String collection;
    public Product(){}
    
    public String getName(){
        return name;
    }    
    public String getPrice(){
        return price;
    }    
    public String getCollection(){
        return collection;
    }
    
}

class Pagination{
    //this class was never used :-P
    private String current_page;
    private String per_page;
    private String total;
    public Pagination(){}
}

class Result{
    private Double total_amount;
    private Double total_after_discount;
    
    public void setTotal_amount(Double total_amount){
        this.total_amount =  total_amount;
    }  
    public Double getTotal_amount(){
        return  total_amount;
    }   
    public void setTotal_after_discount(Double total_after_discount){
        this.total_after_discount =  total_after_discount;
    }
}

class Reader{
    /*
    *Sorry for this mess. An external library would had made these easier.
    */
    public static String readURL(String urlString) throws Exception {
        URL url = new URL(urlString);
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

        String finalLine = "";
        String inputLine;
        while ((inputLine = in.readLine()) != null)
        finalLine += inputLine;
        in.close();
        return finalLine;
    }
    
    public static Input readSTD_IN() {
         Scanner sc = new Scanner(System.in);
        String inputString = "";
        while (sc.hasNext()) {
            inputString += sc.nextLine();
        }
        Gson gson = new Gson();
        return gson.fromJson(inputString, Input.class);
    }
}

class Input{
    String id;
    String discount_type;
    String discount_value;
    String collection;
    String cart_value;
    String product_value;
    
    public Input(){}
    
    public String getId(){
        return id;
    }
    
    public String getDiscount_type(){
        return discount_type;
    }
    
    public String getDiscount_value(){
        return discount_value;
    }
    
    public String getCollection(){
        return collection;
    }    
    public String getCart_value(){
        return cart_value;
    }    
    public String getProduct_value(){
        return product_value;
    }
}

class Constants{
    // I like to keep my static constants separate
    public static final String API_URL = "http://backend-challenge-fall-2018.herokuapp.com/carts.json";
    
}

class NegativeTotalAfterDiscountException extends Exception{
    public NegativeTotalAfterDiscountException (){
        super();
    }
    public NegativeTotalAfterDiscountException (String message){
        super(message);
    }
}