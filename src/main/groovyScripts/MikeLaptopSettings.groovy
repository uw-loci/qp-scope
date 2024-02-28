guiscript=true

// Script to set working default preferences for a single instrument, for use in the qp-scope extension
// Instrument: CAMM

preferenceSettings = getQuPath().getPreferencePane().getPropertySheet().getItems()
preferenceSettings.find{it.getName() == "PycroManager Path"}.setValue("C:\\ImageAnalysis\\QPExtension0.5.0\\qp-scope\\src\\main\\pythonScripts/4x_bf_scan_pycromanager.py")
preferenceSettings.find{it.getName() == "Python Environment"}.setValue("C:\\Anaconda\\envs\\paquo")
preferenceSettings.find{it.getName() == "Projects Folder"}.setValue("C:\\ImageAnalysis\\QPExtension0.5.0\\data\\slides")
preferenceSettings.find{it.getName() == "Extension Location"}.setValue("C:\\ImageAnalysis\\QPExtension0.5.0\\qp-scope")
preferenceSettings.find{it.getName() == "Tissue Detection Script"}.setValue("DetectTissue.groovy")
preferenceSettings.find{it.getName() == "First Scan Type"}.setValue("4x_bf")
preferenceSettings.find{it.getName() == "Second Scan Type"}.setValue("20x_bf")
preferenceSettings.find{it.getName() == "Tile Handling Method"}.setValue("None") // "Zip" or "Delete" are functional, anything else does nothing
preferenceSettings.find{it.getName() == "1st scan pixel size um"}.setValue(1.105)
preferenceSettings.find{it.getName() == "Macro image px size"}.setValue(7.2)
preferenceSettings.find{it.getName() == "2nd scan pixel size um"}.setValue(0.5)
preferenceSettings.find{it.getName() == "Camera Frame Width #px"}.setValue(1392)
preferenceSettings.find{it.getName() == "Camera Frame Height #px"}.setValue(1040)
preferenceSettings.find{it.getName() == "Tile Overlap Percent"}.setValue(0)
preferenceSettings.find{it.getName() == "Compression type"}.setValue(OMEPyramidWriter.CompressionType.J2K_LOSSY)
print("If no errors, update completed successfully.")

import qupath.lib.images.writers.ome.OMEPyramidWriter