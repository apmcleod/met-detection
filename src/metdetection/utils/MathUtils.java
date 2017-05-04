package metdetection.utils;

/**
 * A static class of some utility functions that may need to used from multiple places.
 * 
 * @author Andrew McLeod
 */
public class MathUtils {
	/**
	 * A private constructor since this class should never be instantiated.
	 */
	private MathUtils() {}
	
	/**
	 * Evaluate a Gaussian window function with the given mean range and standard deviation. The formula is:
	 * 
	 * G(m1, m2, s) = e ^ [(-1 / 2) * ([m2 - m1] / s) ^ 2]
	 * 
	 * @param mean1 The low end of the mean range.
	 * @param mean2 The high end of the mean range.
	 * @param std The standard deviation.
	 * @return The value of the Gaussian window function.
	 */
	public static double gaussianWindow(double mean1, double mean2, double std) {
		double fraction = (mean2 - mean1) / std;
		double exponent = - (fraction * fraction) / 2.0;
		return Math.exp(exponent);
	}

	/**
	 * Get the index of the first occurrence of the maximum value in the given array.
	 * 
	 * @param array The array whose max we will find.
	 * @return The index of the first occurrence of the maximum value of the given array. Or,
	 * -1 if the maximum vlaue is {@link Double#NEGATIVE_INFINITY} or the array has length 0.
	 */
	public static int getMaxIndex(double[] array) {
		double maxVal = Double.NEGATIVE_INFINITY;
		int maxIndex = -1;
		
		for (int i = 0; i < array.length; i++) {
			if (array[i] > maxVal) {
				maxVal = array[i];
				maxIndex = i;
			}
		}
		
		return maxIndex;
	}
	
	/**
	 * Get the greatest common factor of two integers.
	 * 
	 * @param a The first integer.
	 * @param b The second integer.
	 * @return Their greatest common factor.
	 */
	public static int getGCF(int a, int b) {
	   if (b == 0) {
		   return a;
	   }
		   
	   return getGCF(b, a % b);
	}
}
