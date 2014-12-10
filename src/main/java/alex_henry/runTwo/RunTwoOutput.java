package alex_henry.runTwo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.vfs2.FileSystemException;
import org.openimaj.data.dataset.VFSListDataset;
import org.openimaj.feature.FloatFV;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.ml.annotation.Annotated;
import org.openimaj.ml.annotation.AnnotatedObject;
import org.openimaj.ml.annotation.ScoredAnnotation;

public class RunTwoOutput {

	public static void main(String[] args){
		int k = 500;
		
		Set<FImage> trainingImages = new HashSet<FImage>();
		List<Annotated<FImage,String>> trainingAnnotations = new ArrayList<Annotated<FImage,String>>();
		File testingFolder = new File("./images/testing");
		File trainingFolder = new File("./images/training");
		int subs = trainingFolder.listFiles().length - 1;
		int subsAdded = 0;
		
		Map<String,FImage> teImages = new HashMap<String,FImage>();
		for(File f : testingFolder.listFiles()){
			try {
				teImages.put(f.getName(),ImageUtilities.readF(f));
			} catch (IOException e) {
				System.err.println("Unable to read image "+f.getName());
				
			}
		}
	
		for(File subFolder : trainingFolder.listFiles())
		{
			
			VFSListDataset<FImage> trImages;
			try {
				trImages = new VFSListDataset<FImage>(subFolder.getAbsolutePath(), ImageUtilities.FIMAGE_READER);
			} catch (FileSystemException e) {
				e.printStackTrace();
				break;
			}
			
			System.out.println(subsAdded++ + "/" + subs);
			
			int toUse = 2;
			if(toUse > trImages.size()){
				toUse = trImages.size();
			}
			
			for(int i = 0; i < toUse; i++)
			{
				FImage f = trImages.get(i);
				trainingImages.add(f);
				trainingAnnotations.add(new AnnotatedObject<FImage,String>(f,subFolder.getName()));
			}
			
			for(int i = toUse; i < trImages.size(); i++)
			{
				trainingAnnotations.add(new AnnotatedObject<FImage,String>(trImages.get(i),subFolder.getName()));
			}
			
		}
		//get ALL the vectors!
		PatchExtractor extractor = new PatchExtractor();
		Set<FloatFV> vectors = new HashSet<FloatFV>();
		int patched = 0;
		int nanVectors = 0;
		for(FImage f : trainingImages){
			for(FImage patch : extractor.getPatches(f)){
				FloatFV vector = extractor.getVector(patch);
				if(AppTwo.isNaNy(vector)){
					nanVectors++;
				} else {
					vectors.add(vector);
				}
			}
			patched++;
			if(patched%100 == 0){
				System.out.println(patched + " images patched");
			}
		}
		System.out.println("NaN containing vectors: " + nanVectors);
		//System.out.println("Patching complete");
		System.out.println(vectors.size() + " vectors created");
		
		
		//figure out the k means
		Set<FloatFV> vocabulary = new KMeansFloatFV().getMeans(k, vectors);
		System.out.println("I did it!");
		/*
		 * KMeans class produces a bag-of-visual-words feature using the patches produced by the PatchExtractor
		 * */
		FloatFV[] array = new FloatFV[vocabulary.size()];
		ClassifierFloatFV classifier = new ClassifierFloatFV(Arrays.asList(vocabulary.toArray(array)));
		
		classifier.train(trainingAnnotations);
			
		File output = new File("./Output/RunTwo.txt");
		FileWriter fWriter = null;
		try {
			fWriter = new FileWriter(output);
		} catch (IOException e) {
			e.printStackTrace();
		}
		PrintWriter pWriter = new PrintWriter(fWriter);
		
		
		for(String key : teImages.keySet())
		{
			List<ScoredAnnotation<String>> predictions = classifier.annotate(teImages.get(key));
			float confidence = 0f; String bestPrediction = null;
			for(ScoredAnnotation<String> anno : predictions)
			{
				if(anno.confidence > confidence)
				{
					bestPrediction = anno.annotation;
					confidence = anno.confidence;
				}
			}
			pWriter.println(key + " " + bestPrediction);
		}
	}
}