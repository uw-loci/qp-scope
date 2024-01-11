import scipy.io
import numpy as np
import cv2
from skimage import io
matfile = scipy.io.loadmat('sequencenew.mat')
image_data = matfile['sequencenew']

data = np.transpose(image_data, (2, 0, 1))
print(data.dtype)
io.imsave('Stack.tif' ,data)

