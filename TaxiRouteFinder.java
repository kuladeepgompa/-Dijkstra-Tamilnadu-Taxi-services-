import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class TaxiRouteFinder {
    static class Edge {
        String destination;
        double distance;
        double time;
        double fare;
        String vehicleType;
        String weatherCondition;

        Edge(String destination, double distance, double time, double fare, 
             String vehicleType, String weatherCondition) {
            this.destination = destination;
            this.distance = distance;
            this.time = time;
            this.fare = fare;
            this.vehicleType = vehicleType;
            this.weatherCondition = weatherCondition;
        }
    }

    static class PathNode implements Comparable<PathNode> {
        String location;
        double distance;
        List<String> path;
        double totalTime;
        double totalFare;
        List<String> vehicleTypes;
        List<String> weatherConditions;

        PathNode(String location, double distance, List<String> path, double totalTime, 
                double totalFare, List<String> vehicleTypes, List<String> weatherConditions) {
            this.location = location;
            this.distance = distance;
            this.path = path;
            this.totalTime = totalTime;
            this.totalFare = totalFare;
            this.vehicleTypes = vehicleTypes;
            this.weatherConditions = weatherConditions;
        }

        @Override
        public int compareTo(PathNode other) {
            return Double.compare(this.distance, other.distance);
        }
    }

    private static Map<String, List<Edge>> buildGraph(String filePath) throws IOException {
        Map<String, List<Edge>> graph = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String headerLine = br.readLine();
            if (headerLine == null) {
                throw new IOException("Empty CSV file");
            }
            
            String[] headers = headerLine.split(",");
            
            // Find column indices
            Map<String, Integer> columnIndices = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                columnIndices.put(headers[i].trim(), i);
            }

            // Verify required columns
            String[] requiredColumns = {
                "Pickup_Location", "Drop_Location", "Distance_km", 
                "Fare_INR", "Travel_Time_hrs", "Vehicle_Type", "Weather_Condition"
            };
            
            for (String column : requiredColumns) {
                if (!columnIndices.containsKey(column)) {
                    throw new IOException("Missing required column: " + column);
                }
            }

            String line;
            int lineNumber = 1;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;
                
                try {
                    String[] values = line.split(",");
                    if (values.length < headers.length) {
                        System.out.println("Warning: Skipping incomplete line " + lineNumber);
                        continue;
                    }

                    String pickup = values[columnIndices.get("Pickup_Location")].trim();
                    String drop = values[columnIndices.get("Drop_Location")].trim();
                    String distanceStr = values[columnIndices.get("Distance_km")].trim();
                    String fareStr = values[columnIndices.get("Fare_INR")].trim();
                    String timeStr = values[columnIndices.get("Travel_Time_hrs")].trim();
                    String vehicleType = values[columnIndices.get("Vehicle_Type")].trim();
                    String weather = values[columnIndices.get("Weather_Condition")].trim();

                    // Skip if essential fields are empty
                    if (pickup.isEmpty() || drop.isEmpty()) {
                        System.out.println("Warning: Skipping line " + lineNumber + " - missing location data");
                        continue;
                    }

                    // Parse numeric values with validation
                    double distance = parseDoubleWithDefault(distanceStr, -1);
                    double fare = parseDoubleWithDefault(fareStr, -1);
                    double time = parseDoubleWithDefault(timeStr, -1);

                    // Skip if any numeric values are invalid
                    if (distance <= 0 || fare <= 0 || time <= 0) {
                        System.out.println("Warning: Skipping line " + lineNumber + 
                                         " - invalid numeric values: Distance=" + distanceStr + 
                                         ", Fare=" + fareStr + ", Time=" + timeStr);
                        continue;
                    }

                    // Add edge to graph
                    graph.putIfAbsent(pickup, new ArrayList<>());
                    graph.get(pickup).add(new Edge(drop, distance, time, fare, vehicleType, weather));
                    
                } catch (Exception e) {
                    System.out.println("Warning: Error processing line " + lineNumber + ": " + e.getMessage());
                }
            }
        }
        
        if (graph.isEmpty()) {
            throw new IOException("No valid routes found in the CSV file");
        }
        
        return graph;
    }

    private static double parseDoubleWithDefault(String value, double defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static PathNode findShortestPath(Map<String, List<Edge>> graph, String source, String destination) {
        PriorityQueue<PathNode> pq = new PriorityQueue<>();
        Set<String> visited = new HashSet<>();
        
        List<String> initialPath = new ArrayList<>();
        initialPath.add(source);
        pq.offer(new PathNode(source, 0, initialPath, 0, 0, 
                 new ArrayList<>(), new ArrayList<>()));

        while (!pq.isEmpty()) {
            PathNode current = pq.poll();
            
            if (current.location.equals(destination)) {
                return current;
            }

            if (visited.contains(current.location)) {
                continue;
            }
            
            visited.add(current.location);

            for (Edge edge : graph.getOrDefault(current.location, new ArrayList<>())) {
                if (!visited.contains(edge.destination)) {
                    List<String> newPath = new ArrayList<>(current.path);
                    List<String> newVehicleTypes = new ArrayList<>(current.vehicleTypes);
                    List<String> newWeatherConditions = new ArrayList<>(current.weatherConditions);
                    
                    newPath.add(edge.destination);
                    newVehicleTypes.add(edge.vehicleType);
                    newWeatherConditions.add(edge.weatherCondition);
                    
                    pq.offer(new PathNode(
                        edge.destination,
                        current.distance + edge.distance,
                        newPath,
                        current.totalTime + edge.time,
                        current.totalFare + edge.fare,
                        newVehicleTypes,
                        newWeatherConditions
                    ));
                }
            }
        }
        return null;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("Enter source location: ");
        String source = scanner.nextLine().trim();
        
        System.out.print("Enter destination location: ");
        String destination = scanner.nextLine().trim();

        try {
            String filePath = "E:/OOPS project/New folder/cleaned_file.csv"; // Update this path
            Map<String, List<Edge>> graph = buildGraph(filePath);

            // Print available locations
            System.out.println("\nAvailable locations:");
            Set<String> locations = new HashSet<>(graph.keySet());
            graph.values().forEach(edges -> 
                edges.forEach(edge -> locations.add(edge.destination)));
            locations.forEach(System.out::println);

            // Verify locations exist
            if (!graph.containsKey(source)) {
                System.out.println("\nError: Source location '" + source + "' not found in database");
                System.out.println("Please choose from the available locations listed above.");
                return;
            }

            PathNode shortestPath = findShortestPath(graph, source, destination);
            
            if (shortestPath == null) {
                System.out.println("\nNo path found between '" + source + "' and '" + destination + "'");
                return;
            }

            // Print detailed path information
            System.out.println("\nShortest Path Details:");
            System.out.println("Complete Route:");
            for (int i = 0; i < shortestPath.path.size() - 1; i++) {
                System.out.printf("%s -> %s (Vehicle: %s, Weather: %s)%n",
                    shortestPath.path.get(i),
                    shortestPath.path.get(i + 1),
                    shortestPath.vehicleTypes.get(i),
                    shortestPath.weatherConditions.get(i));
            }
            
            System.out.printf("\nTotal Distance: %.2f km%n", shortestPath.distance);
            System.out.printf("Total Time: %.2f hours%n", shortestPath.totalTime);
            System.out.printf("Total Fare: ₹%.2f%n", shortestPath.totalFare);

        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }
}