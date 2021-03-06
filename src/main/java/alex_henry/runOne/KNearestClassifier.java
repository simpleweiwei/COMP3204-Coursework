package alex_henry.runOne;

import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

import org.openimaj.data.dataset.ListDataset;
import org.openimaj.feature.FloatFV;
import org.openimaj.feature.FloatFVComparison;
import org.openimaj.image.FImage;

public class KNearestClassifier {

	HashMap<FloatFV, String> classes; 

	public KNearestClassifier()
	{
		classes = new HashMap<FloatFV,String>();
	}

	//kValue - number of nearest neighbours to compare feature vector to
	public String classify(FImage image,int kValue)
	{
		TinyImage tiny = new TinyImage(image,16,16);
		return classify(tiny,kValue);
	}
	
	public String classify(TinyImage tiny,int kValue){
		
		FloatFV v = tiny.getVector();
		PriorityQueue<FloatFV> q = new PriorityQueue<FloatFV>(1,new VectorComparison(v));

		for(FloatFV vector : classes.keySet())
		{
			q.add(vector);
		}

		HashMap<String, Integer> classCount = new HashMap<String, Integer>();
		//get the k nearest neighbours
		for(int i = 0; i < kValue ; i++){
			FloatFV neighbour = q.poll();
			String neighbourClass = classes.get(neighbour);
			if(classCount.containsKey(neighbourClass))
			{
				classCount.put(neighbourClass, classCount.get(neighbourClass) + 1); 
			}
			else
			{
				classCount.put(neighbourClass, new Integer(1));
			}
		}
		//determine class using them
		String targetClass = null;
		Integer n = 0;
		//TODO improve case when multiple classes have the same count
		for(String s : classCount.keySet())
		{
			Integer sNum = classCount.get(s);
			if(sNum > n)
			{
				targetClass = s;
				n = sNum;
			}
		}
		
//		for(String s : classCount.keySet()){
//			System.out.println(s + " : " + classCount.get(s));
//		}
		
		return targetClass;
	}

	//Used to train classifier
	public void addClassValues(ListDataset<TinyImage> group,String setName)
	{
		for(TinyImage i : group){
			classes.put(i.getVector(), setName);
		}

	}
	
	public void addClassValue(TinyImage img, String setName){
		classes.put(img.getVector(), setName);
	}

	class VectorComparison implements Comparator<FloatFV>{

		FloatFV target;

		public VectorComparison(FloatFV target){
			this.target = target;
		}

		@Override
		public int compare(FloatFV arg0, FloatFV arg1) {
			FloatFVComparison comp = FloatFVComparison.EUCLIDEAN;
			Double dist0 = comp.compare(arg0, target);
			Double dist1 = comp.compare(arg1, target);
			return dist0.compareTo(dist1);
		}

	}

}
