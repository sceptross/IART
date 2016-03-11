package cityparser;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts the name, population, longitude and latitude of each Portuguese county.
 */
public class CityParser {
    /**
     * The URL to get the county names and populations.
     */
    private final static String citiesUrl = "https://pt.wikipedia.org/wiki/Lista_de_municípios_de_Portugal_por_população";
    /**
     * This is my Google Maps Geocoding API Key. YOU ARE PROHIBITED TO USE THIS IN ANY WAY OTHER THAN IN THE CONTEXT
     * OF THIS APPLICATION.
     */
    private final static String googleApiKey = "AIzaSyDz40hA-iPh957WG8FXY1G6jMOxgKHTAzI";

    /**
     * List with all the cities.
     */
    private List<City> cities = new ArrayList<>();

    /**
     * String to save the html of the page with the names and population.
     */
    private String html;

    /**
     * Returns all the cities with all the information filled.
     * @param fileName file to save / read the information.
     * @return List with all the information.
     */

    public List<City> getCities(String fileName){
        File f = new File(fileName);
        if(f.exists() && !f.isDirectory())
            cities = unserializeCities(fileName);
        else {
            System.out.println("Extracting data from the Web...");
            extractHtml();
            parseCities();
            parseCoords();
            serializeCities(fileName);
        }
        return cities;
    }

    /**
     * Get the coords of each city from the Google Maps API.
     */
    private void parseCoords() {
        String json;
        try {
            /* Faster than not using forEach */
            int i = 1;
            for (City city : cities) {
                System.out.println("Extracting city coordinates " + i + " of " + cities.size() + ". Please wait...");
                ++i;
                Thread.sleep(50); // We need this in order not to overload the Google Maps API with queries
                json = extractCityJson(city);
                Coord latlong = getLatLong(json);
                city.setLatitude(latlong.latitude);
                city.setLongitude(latlong.longitude);
            }
        } catch(IOException | InterruptedException e){
            e.printStackTrace();
        }
    }

    /**
     * Parses the JSON provided by the Google Maps API, extracting the latitude and longitude.
     * @param json The JSON with the info.
     * @return The latitude and longitude of the city.
     */
    private Coord getLatLong(String json) {
        JSONObject obj = new JSONObject(json);
        JSONObject results = (JSONObject) obj.getJSONArray("results").get(0);
        JSONObject geometry = results.getJSONObject("geometry");
        JSONObject location = geometry.getJSONObject("location");
        return new Coord(location.getDouble("lat"), location.getDouble("lng"));
    }

    /**
     * Queries the Google Maps API for the latitude / longitude information for a city.
     * @param city The city to get the info from.
     * @return The JSON.
     * @throws IOException Never happens. Come on, this is a high-level language. Please.
     */
    private String extractCityJson(City city) throws IOException {
        String cityName = URLEncoder.encode(city.getName(), "utf-8");
        URL url = new URL("https://maps.googleapis.com/maps/api/geocode/json?address=" + cityName +
                ",Portugal&key=" + googleApiKey);
        InputStream is = (InputStream)url.getContent();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        StringBuilder sb = new StringBuilder();
        while((line = br.readLine()) != null)
            sb.append(line);
        return sb.toString();
    }

    /**
     * Parses the city names and populations from the html string.
     */
    private void parseCities() {
        Document doc = Jsoup.parse(html);
        Elements citiesHtml = doc.getElementsByAttributeValue("style", "text-align: center;");

        for (Element aCitiesHtml : citiesHtml) {
            String name = aCitiesHtml.children().get(1).text();
            String population = aCitiesHtml.children().get(2).text();
            cities.add(new City(name, parsePopulation(population)));
        }

        System.out.println("Finished extracting city names and populations");
    }

    /**
     * Converts the information about a city's population to an integer.
     * @param population Population as a string.
     * @return Population as an integer.
     */
    private int parsePopulation(String population){
        String ret = "";

        for(int i = 0; i < population.length(); ++i)
            if(Character.isDigit(population.charAt(i)))
                ret+=population.charAt(i);

        return Integer.parseInt(ret);
    }

    /**
     * Extracts the HTML from the URL provided in citiesURL.
     */
    private void extractHtml() {
        try {
            URL url = new URL(citiesUrl);
            InputStream is = (InputStream)url.getContent();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuilder sb = new StringBuilder();
            while((line = br.readLine()) != null)
                sb.append(line);
            html = sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Serializes the city information.
     * @param fileName Name of the file to serialize the info to.
     */
    private void serializeCities(String fileName) {
        try {
            FileOutputStream fileOut = new FileOutputStream(fileName);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(cities);
            out.close();
            fileOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Unserializes the city information.
     * @param fileName Name of the file to unserialize the info from.
     * @return List with the cities.
     */
    private List<City> unserializeCities(String fileName) {
        List<City> cities;
        try {
            FileInputStream fileIn = new FileInputStream(fileName);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            cities = (List<City>) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return cities;
    }

    /**
     * Holds the latitude and longitude for a city.
     */
    public class Coord {
        /**
         * Latitude.
         */
        private double latitude;

        /**
         * Longitude.
         */
        private double longitude;

        /**
         * Coord constructor.
         * @param latitude Latitude.
         * @param longitude Longitude.
         */
        public Coord(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
