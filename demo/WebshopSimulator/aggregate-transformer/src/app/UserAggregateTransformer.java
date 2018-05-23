package app;

// Transformations
// * The PO box number - optional as a user may reside in a rural area, Post Town (printed in capitals),
//   and post code, also in all caps, gets composed into a full address string
// * The country string is converted to an integer field phone indicating the country code the telephone string corresponds to

import org.json.JSONObject;
import vbb.dbupgradinator.AbstractAggregateTransformer;

public class UserAggregateTransformer extends AbstractAggregateTransformer {

    // This class implements the transformation interface declared in DBUpgradinator
    // Everything hinges on the fact that when casting an object to its abstract parent class,
    // calling on an abstract method results in calling on the override method

    public UserAggregateTransformer(String current, String next) {
        super(current, next);
    }

    // The transformation method may utilize private helper functions
    private static String checkEmptyness(String d) {
        if (d.isEmpty()) { return ""; }
        return d + "; ";
    }
    private int getCountryCode(String country) {
        // In order to be able tot transform an aggregate, you often need some
        // concrete domain knowledge of the data stored by the application.
        int phone = -1;
        String checker = country.toUpperCase();
        switch (checker) {
            case "UK" :
                phone = 44;
                break;
            case "AUS" :
                phone = 61;
                break;
            case "IRE" :
                phone = 353;
                break;
            default :
                break;
        }
        return phone;
    }

    /**
     * Both parameters are strings either returned from or going into a DB query
     * @return String - the new aggregate based on the input
     * @param val The value, which is a generic object
    */
    public String transformAggregate(String val) {
      // Edit JSON object - how to
      JSONObject jo = new JSONObject(val);
      String countryAbb = jo.getString("country");
      String fullAddress =  checkEmptyness(jo.getString("streetAddress")) +
              checkEmptyness(jo.getString("city")) +
              checkEmptyness(jo.getString("state")) +
              checkEmptyness(jo.getString("zipCode")) +
              checkEmptyness(countryAbb);
      jo.put("address", fullAddress);
      jo.remove("streetAddress");
      jo.remove("city");
      jo.remove("state");
      jo.remove("zipCode");
      jo.put("country", getCountryCode(countryAbb)); // Change the field usage and type
      return jo.toString();
    }
}
