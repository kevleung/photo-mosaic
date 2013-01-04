package com.example.photomosaic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class MainActivity extends Activity {
	private static int RESULT_LOAD_IMAGE = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Button buttonLoadImage = (Button) findViewById(R.id.buttonLoadPicture);
        buttonLoadImage.setOnClickListener(new View.OnClickListener() {
		
            @Override
            public void onClick(View arg0) {
                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });
	}
	
	/**
	 * Calculate the average colour of the image; find the total RGB values and the
	 * total number of pixels making up the image to find the colour average.  
	 * @param image is a Bitmap object.
	 * @return ArrayList<R, G, B> with each of R,G,B being Integers.
	 */
	private ArrayList<Integer> colorAverage(Bitmap image){
		int width = image.getWidth();
		int height = image.getHeight();
		int numPixels = width * height;
		int red = 0;
		int green = 0;
		int blue = 0;
		
		int[] pixels = new int[numPixels];
		image.getPixels(pixels, 0, width, 0, 0, width, height);
		for (int x = 0; x < numPixels; x++){
			red += Color.red(pixels[x]);
			green += Color.green(pixels[x]);
			blue += Color.blue(pixels[x]);
		}
		ArrayList<Integer> average = new ArrayList<Integer>();
		average.add(red/numPixels);
		average.add(green/numPixels);
		average.add(blue/numPixels);
		return average;
	}
	
	/**
	 * Iterate through all the dali images to find their colorAverage and 
	 * insert into HashMap.
	 * @return HashMap with an array containing RGB values as key and the corresponding Bitmap as value.
	 */
	private HashMap<ArrayList<Integer>, Bitmap> saveImgAvg() {
		HashMap<ArrayList<Integer>, Bitmap> imgAvg = new HashMap<ArrayList<Integer>, Bitmap>();	
		int i;
		//All images in resources folder are named "dali1" up to "dali50".
		//All images are art works of Salvador Dali, the Spanish painter.
		Field[] drawables = android.R.drawable.class.getFields();
		for (i = 0; i < drawables.length; i++) {
			int res = getResources().getIdentifier("dali" + i , "drawable", getPackageName()); 
			if (res != 0){
				Bitmap bitmap = BitmapFactory.decodeResource(getResources(), res);
				imgAvg.put(colorAverage(bitmap), bitmap);
			}
		}
		return imgAvg;
	}
	
	/**
	 * Divide the given picture into 4 quadrants and add each to the ArrayList.
	 * @param picture, a Bitmap object
	 * @return An array of 4 quadrants
	 */
	private ArrayList<Bitmap> divideQuadrants(Bitmap image1){
		//convert image1 into a mutable bitmap
		ArrayList<Bitmap> quadrants = new ArrayList<Bitmap>();
		int width = image1.getWidth();
		int height = image1.getHeight();
		
		Bitmap upperLeft = Bitmap.createBitmap(image1, 0, 0, width/2, height/2);
		Bitmap upperRight = Bitmap.createBitmap(image1, width/2, 0, width/2, height/2);
		Bitmap lowerLeft = Bitmap.createBitmap(image1, 0, height/2, width/2, height/2);
		Bitmap lowerRight = Bitmap.createBitmap(image1, width/2, height/2, width/2, height/2);
		
		quadrants.add(upperLeft);
		quadrants.add(upperRight);
		quadrants.add(lowerLeft);
		quadrants.add(lowerRight);
		
		return quadrants;
	}
	
	/**
	 * Make the mosaic; overlay the closest dali image on the corresponding portion of
	 * the chosen image which is being transformed into a mosaic recursively. 
	 * @param orgImg: a Bitmap, the image that was chosen by the user.
	 * @param dalis: HashMap with key:RGB average of a dali image and value: the associated Bitmap 
	 * @return the mosaic.
	 */
    private Bitmap makeMosaic(Bitmap orgImg, HashMap<ArrayList<Integer>, Bitmap> dalis){
    	Canvas canvas = new Canvas(orgImg);
    	int orgWidth = orgImg.getWidth();
    	int orgHeight = orgImg.getHeight();
    	
    	//No need to divide; loop over all color averages of each dali 
    	//image to find the closest matching image and resize it.
    	if (orgWidth < 10 || orgHeight < 10){
    		ArrayList<Integer> avgOriginalImage = colorAverage(orgImg);
    		int originalRed = avgOriginalImage.get(0);
    		int originalGreen = avgOriginalImage.get(1);
    		int originalBlue = avgOriginalImage.get(2);
    		double smallestDistance = Double.POSITIVE_INFINITY;
    		double temp;
    		ArrayList<Integer> matchingKey = new ArrayList<Integer>();
    		
    		for (ArrayList<Integer> key : dalis.keySet()){
    			temp = Math.sqrt(Math.pow((double)(key.get(0) - originalRed), 2) + 
    					Math.pow((double)(key.get(1) - originalGreen), 2) +
    					Math.pow((double)(key.get(2) - originalBlue), 2));
    			if (temp < smallestDistance){
    				smallestDistance = temp;
    				matchingKey = key;
    			}
    		}
    		//Resize the matching image to the correct dimensions.
    		Bitmap closestImage = saveImgAvg().get(matchingKey);
    		return Bitmap.createScaledBitmap(closestImage, orgWidth, orgHeight, false);
    	//The image has to be divided.
    	}else{
    		ArrayList<Bitmap> quadrants = divideQuadrants(orgImg);
    		canvas.drawBitmap(makeMosaic(quadrants.get(0), dalis), 0, 0, null);
    		canvas.drawBitmap(makeMosaic(quadrants.get(1), dalis), orgWidth/2, 0, null);
    		canvas.drawBitmap(makeMosaic(quadrants.get(2), dalis), 0, orgHeight/2, null);
    		canvas.drawBitmap(makeMosaic(quadrants.get(3), dalis), orgWidth/2, orgHeight/2, null);

    		return orgImg;
    	}
    }
    
    /**
     * createMosaic() calls makeMosaic() to obtain the actual mosaic from the chosen
     * image.  The output to makeMosaic() is a Bitmap which is in turn written to a
     * newly created File.  This is then inserted into the Android gallery. 
     * @param chosenImage: The Bitmap of the image that the user chose.
     * @param daliAverages: HashMap with key:RGB average of a dali image and value: the associated Bitmap
     * @param filename: The name of chosenImage.  
     * @return the mosaic.
     * @throws IOException 
     */
    private Bitmap createMosaic(Bitmap chosenImage, HashMap<ArrayList<Integer>, Bitmap> daliAverages, 
    		String filePath) throws IOException{
    	//Make the mosaic from the chosen image.
    	Bitmap mosaic = makeMosaic(chosenImage, daliAverages);
    	
    	//Get the name of the chosen image.
    	String[] chosenPath = filePath.split("/");
    	File path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File file = new File(path, chosenPath[chosenPath.length - 1]);
        FileOutputStream out = new FileOutputStream(file);
		mosaic.compress(Bitmap.CompressFormat.PNG, 100, out);
		
		// Tell the media scanner about the new file so that it is
		// immediately available to the user.
		//This code snippet(line 190-196) was taken from 
		//http://developer.android.com/reference/android/os/Environment.html
		MediaScannerConnection.scanFile(this,
		        new String[] { file.toString() }, null,
		        new MediaScannerConnection.OnScanCompletedListener() {
		    public void onScanCompleted(String path, Uri uri) {
		        Log.i("ExternalStorage", "Scanned " + path + ":");
		        Log.i("ExternalStorage", "-> uri=" + uri);
		    }
		});		
    	return mosaic;
    }
	
    /**
     * Open the Android gallery and let the user choose an image. A mosaic will be
     * created based on the chosen image, saved and inserted to the gallery.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
 
            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();
 
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
             
            //Create a mutable Bitmap and display the mosaic to screen.
            Bitmap originalImage = BitmapFactory.decodeFile(picturePath);
            Bitmap originalMutable = originalImage.copy(originalImage.getConfig(), true);
            ImageView imageView = (ImageView) findViewById(R.id.imgView);
            try {
				imageView.setImageBitmap(createMosaic(originalMutable, saveImgAvg(), picturePath));
			} catch (IOException e) {
				e.printStackTrace();
			}            
        }    
    }
}
