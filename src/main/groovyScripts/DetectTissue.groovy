setImageType('BRIGHTFIELD_H_E');
setColorDeconvolutionStains('{"Name" : "H&E default", "Stain 1" : "Hematoxylin", "Values 1" : "0.65111 0.70119 0.29049", "Stain 2" : "Eosin", "Values 2" : "0.2159 0.8012 0.5581", "Background" : " 255 255 255"}');
//Set pixel size
setPixelSizeMicrons(2.0, 2.0)
//createFullImageAnnotation(true)
createAnnotationsFromPixelClassifier("C:/ImageAnalysis/python/Tissue-lowres.json", 500000.0, 50000.0, "SPLIT", "SELECT_NEW")
