package logic.utility;

import org.apache.commons.math3.util.Pair;

/**
 * Builds and handles an array with a flexible number of dimensions
 * 
 * @author M. Lang
 *
 */
public class MultiDimensionalArrayProducer {

	/**
	 * Creates the array with the multiple dimensions. Number of entries of size
	 * defines the number of dimensions.
	 * 
	 * @param size
	 *            Sizes of the respective dimensions
	 * @return the array with multiple dimensions, entries are doubles
	 */
	public static Object[] createDoubleArray(int... size) {
		Object[] array = new Object[size[0]];
		createDoubleArray(1, array, size);
		return array;
	}

	private static void createDoubleArray(int position, Object[] array, int... size) {
		for (int i = 0; i < array.length; i++) {

			if (size.length == position + 1) {
				Object[] subArray = new Double[size[position]];
				array[i] = subArray;
			} else {
				Object[] subArray = new Object[size[position]];
				array[i] = subArray;
				createDoubleArray(position + 1, subArray, size);
			}

		}
	}
	
	/**
	 * Creates the array with the multiple dimensions. Number of entries of size
	 * defines the number of dimensions.
	 * 
	 * @param size
	 *            Sizes of the respective dimensions
	 * @return the array with multiple dimensions, entries are double pairs
	 */
	public static Object[] createDoublePairArray(int... size) {
		Object[] array = new Object[size[0]];
		createDoublePairArray(1, array, size);
		return array;
	}

	private static void createDoublePairArray(int position, Object[] array, int... size) {
		for (int i = 0; i < array.length; i++) {

			if (size.length == position + 1) {
				Object[] subArray = new Pair[size[position]];
				array[i] = subArray;
			} else {
				Object[] subArray = new Object[size[position]];
				array[i] = subArray;
				createDoublePairArray(position + 1, subArray, size);
			}

		}
	}

	/**
	 * Allows to read a specific entry of a given array with multiple dimensions
	 * 
	 * @param array
	 *            The respective array
	 * @param path
	 *            The entry-path
	 * @return The value
	 */
	public static Double readDoubleArray(Object[] array, int... path) {
		return readDoubleArray(0, array, path);
	}

	private static Double readDoubleArray(int position, Object[] array, int... path) {
		if (path.length == 0) {
			return null;
		} else if (path.length == position + 1) {
			if (array instanceof Double[]) {
				// System.out.println("I return:"+(Double)
				// array[path[position]]);
				return (Double) array[path[position]];
			} else {
				return null;
			}
		} else {
			Object[] subArray = (Object[]) array[path[position]];
			return readDoubleArray(position + 1, subArray, path);
		}
	}
	
	/**
	 * Allows to read a specific entry of a given array with multiple dimensions
	 * 
	 * @param array
	 *            The respective pair array
	 * @param path
	 *            The entry-path
	 * @return The value
	 */
	public static Pair<Double, Double> readDoublePairArray(Object[] array, int... path) {
		return readDoublePairArray(0, array, path);
	}

	private static  Pair<Double, Double> readDoublePairArray(int position, Object[] array, int... path) {
		if (path.length == 0) {
			return null;
		} else if (path.length == position + 1) {
			if (array instanceof Pair[]) {
				// System.out.println("I return:"+(Double)
				// array[path[position]]);
				return (Pair<Double, Double>) array[path[position]];
			} else {
				return null;
			}
		} else {
			Object[] subArray = (Object[]) array[path[position]];
			return readDoublePairArray(position + 1, subArray, path);
		}
	}

	/**
	 * Writes a value at a specific position of an array with multiple
	 * dimensions
	 * 
	 * @param value
	 *            The respective value
	 * @param array
	 *            The respective array with multiple dimensions
	 * @param path
	 *            The entry-path
	 */
	public static void writeToDoubleArray(Double value, Object[] array, int... path) {
		writeToDoubleArray(0, value, array, path);
	}

	private static void writeToDoubleArray(int position, Double value, Object[] array, int... path) {
		if (path.length == 0) {
			// EXCEPTION
		} else if (path.length == position + 1) {
			if (array instanceof Double[]) {
				((Double[]) array)[path[position]] = value;
			} else {
				// EXCEPTION
			}
		} else {
			Object[] subArray = (Object[]) array[path[position]];
			writeToDoubleArray(position + 1, value, subArray, path);

		}
	}
	
	/**
	 * Writes a value at a specific position of an array with multiple
	 * dimensions
	 * 
	 * @param value
	 *            The respective pair value
	 * @param array
	 *            The respective array with multiple dimensions
	 * @param path
	 *            The entry-path
	 */
	public static void writeToDoublePairArray(Pair<Double, Double> value, Object[] array, int... path) {
		writeToDoublePairArray(0, value, array, path);
	}

	private static void writeToDoublePairArray(int position, Pair<Double, Double> value, Object[] array, int... path) {
		if (path.length == 0) {
			// EXCEPTION
		} else if (path.length == position + 1) {
			if (array instanceof Pair[]) {
				((Pair<Double, Double>[]) array)[path[position]] = value;
			} else {
				// EXCEPTION
			}
		} else {
			Object[] subArray = (Object[]) array[path[position]];
			writeToDoublePairArray(position + 1, value, subArray, path);

		}
	}

	public static String arrayToString(Object[] array) {
		StringBuilder arrayAsString = new StringBuilder();

		for (int i = 0; i < array.length; i++) {
			arrayAsString.append(arrayToString(Integer.toString(i) + seperator, (Object[]) array[i]));
		}

		return arrayAsString.toString();
	}

	private static String seperator = ";";
	private static String pairSeperator = "-";

	private static StringBuilder arrayToString(String prefix, Object[] array) {

		StringBuilder arrayAsString = new StringBuilder();

		if (array instanceof Double[]) {
			for (int i = 0; i < array.length; i++) {
				Double value = (Double) array[i];

				arrayAsString.append(prefix);
				arrayAsString.append(i);
				arrayAsString.append(seperator);
				arrayAsString.append(value);
				arrayAsString.append(System.lineSeparator());
			}
		} else {
			for (int i = 0; i < array.length; i++) {

				arrayAsString.append(arrayToString(prefix + i + seperator, (Object[]) array[i]));

			}
		}

		return arrayAsString;
	}
	
	public static String pairArrayToString(Object[] array) {
		StringBuilder arrayAsString = new StringBuilder();

		for (int i = 0; i < array.length; i++) {
			arrayAsString.append(pairArrayToString(Integer.toString(i) + seperator, (Object[]) array[i]));
		}

		return arrayAsString.toString();
	}
	
	private static StringBuilder pairArrayToString(String prefix, Object[] array) {

		StringBuilder arrayAsString = new StringBuilder();

		if (array instanceof Pair[]) {
			for (int i = 0; i < array.length; i++) {
				Pair<Double, Double> value = (Pair<Double, Double>) array[i];

				arrayAsString.append(prefix);
				arrayAsString.append(i);
				arrayAsString.append(seperator);
				if(value!=null){
					arrayAsString.append(value.getKey());
					arrayAsString.append(pairSeperator);
					arrayAsString.append(value.getValue());
				}else{
					arrayAsString.append(value);	
				}
				
				arrayAsString.append(System.lineSeparator());
			}
		} else {
			for (int i = 0; i < array.length; i++) {

				arrayAsString.append(pairArrayToString(prefix + i + seperator, (Object[]) array[i]));

			}
		}

		return arrayAsString;
	}
	
	public static Object[] stringToDoubleArray(String arrayAsString, int... size){
        Object[] array = createDoubleArray(size);

        String[] allLines = arrayAsString.split(System.lineSeparator());
        for(int i= 0;i< allLines.length;i++) {
            String[] singleLine = allLines[i].split(seperator);

            for (int j=0;j<singleLine.length; j++){
            	Double value;
            	if(singleLine[singleLine.length-1].equals("null")){
            		value=null;
            	}else{
            		value=Double.valueOf(singleLine[singleLine.length-1]);
            	}
                 
                int[] path = buildPathArray(singleLine);
                writeToDoubleArray(value, array, path);
            }
        }
        return array;
    }

    private static int[] buildPathArray(String[] line){
        int[] path = new int[line.length-1];

        //Last entry is the value. Not needed here
        for(int i=0; i < line.length-1;i++){
            path[i] = Integer.valueOf(line[i]);
        }
        return path;

    }
    
	
	public static Object[] stringToDoublePairArray(String arrayAsString, int... size){
        Object[] array = createDoublePairArray(size);

        String[] allLines = arrayAsString.split(System.lineSeparator());
        for(int i= 0;i< allLines.length;i++) {
            String[] singleLine = allLines[i].split(seperator);

            for (int j=0;j<singleLine.length; j++){
            	Pair<Double, Double> value;
            	if(singleLine[singleLine.length-1].equals("null")){
            		value=null;
            	}else{
            		String[] values = singleLine[singleLine.length-1].split(pairSeperator);
            		value=new Pair<Double, Double>(Double.valueOf(values[0]),Double.valueOf(values[1])) ;
            	}
                 
                int[] path = buildPathArray(singleLine);
                writeToDoublePairArray(value, array, path);
            }
        }
        return array;
    }

}