import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class AllDistances {
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
        double totalTime;
        double totalFare;

        PathNode(String location, double distance, double totalTime, double totalFare) {
            this.location = location;
            this.distance = distance;
            this.totalTime = totalTime;
            this.totalFare = totalFare;
        }

        @Override
        public int compareTo(PathNode other) {
            return Double.compare(this.distance, other.distance);
        }
    }

    private static Map<String, List<Edge>> buildGraph(String filePath) throws IOException {
        Map<String, List<Edge>> graph = new HashMap<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String headerLine = br.readLine();
            if (headerLine == null) {
                throw new IOException("Empty CSV file");
            }
            
            String[] headers = headerLine.split(",");
            Map<String, Integer> columnIndices = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                columnIndices.put(headers[i].trim(), i);
            }

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
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                
                String[] values = line.split(",");
                if (values.length < headers.length) continue;

                String pickup = values[columnIndices.get("Pickup_Location")].trim();
                String drop = values[columnIndices.get("Drop_Location")].trim();
                double distance = parseDoubleWithDefault(values[columnIndices.get("Distance_km")].trim(), -1);
                double fare = parseDoubleWithDefault(values[columnIndices.get("Fare_INR")].trim(), -1);
                double time = parseDoubleWithDefault(values[columnIndices.get("Travel_Time_hrs")].trim(), -1);
                String vehicleType = values[columnIndices.get("Vehicle_Type")].trim();
                String weather = values[columnIndices.get("Weather_Condition")].trim();

                if (pickup.isEmpty() || drop.isEmpty() || distance <= 0 || fare <= 0 || time <= 0) continue;

                graph.putIfAbsent(pickup, new ArrayList<>());
                graph.get(pickup).add(new Edge(drop, distance, time, fare, vehicleType, weather));
            }
        }
        return graph;
    }

    private static double parseDoubleWithDefault(String value, double defaultValue) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static Map<String, PathNode> findShortestPaths(Map<String, List<Edge>> graph, String source) {
        PriorityQueue<PathNode> pq = new PriorityQueue<>();
        Map<String, Double> distances = new HashMap<>();
        Map<String, PathNode> shortestPaths = new HashMap<>();

        pq.offer(new PathNode(source, 0, 0, 0));
        distances.put(source, 0.0);

        while (!pq.isEmpty()) {
            PathNode current = pq.poll();
            String location = current.location;

            if (distances.getOrDefault(location, Double.MAX_VALUE) < current.distance) continue;

            for (Edge edge : graph.getOrDefault(location, new ArrayList<>())) {
                double newDistance = current.distance + edge.distance;
                double newTotalTime = current.totalTime + edge.time;
                double newTotalFare = current.totalFare + edge.fare;

                if (newDistance < distances.getOrDefault(edge.destination, Double.MAX_VALUE)) {
                    distances.put(edge.destination, newDistance);
                    PathNode pathNode = new PathNode(edge.destination, newDistance, newTotalTime, newTotalFare);
                    shortestPaths.put(edge.destination, pathNode);
                    pq.offer(pathNode);
                }
            }
        }
        return shortestPaths;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("Enter source location: ");
        String source = scanner.nextLine().trim();

        try {
            String filePath = "E:/OOPS project/New folder/cleaned_file.csv"; // Update this path
            Map<String, List<Edge>> graph = buildGraph(filePath);

            if (!graph.containsKey(source)) {
                System.out.println("Error: Source location '" + source + "' not found in database");
                return;
            }

            Map<String, PathNode> shortestPaths = findShortestPaths(graph, source);
            
            System.out.println("\nShortest Distances, Time, and Fare from '" + source + "':");
            for (Map.Entry<String, PathNode> entry : shortestPaths.entrySet()) {
                PathNode node = entry.getValue();
                System.out.printf("To %s: %.2f km, %.2f hours, ₹%.2f%n", 
                                  node.location, node.distance, node.totalTime, node.totalFare);
            }

        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }
}
