// Yet anther C++ implementation of EVM, based on OpenCV and Qt. 
// Copyright (C) 2014  Joseph Pan <cs.wzpan@gmail.com>
// 
// This library is free software; you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as
// published by the Free Software Foundation; either version 2.1 of the
// License, or (at your option) any later version.
// 
// This library is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
// 02110-1301 USA
// 


#include "VideoProcessor.h"

VideoProcessor::VideoProcessor(){
	delay = -1;
	rate = 0;
	fnumber = 0;
	length = 0;
	stop = true;
	modify = false;
	curPos = 0;
	curIndex = 0;
	curLevel = 0;
	digits = 0;
	extension = ".avi";
	levels = 4;
	alpha = 50;	// 放大倍数
	lambda_c = 80;
	fl = 0.83;	// 下限频率
	fh = 1;	// 上限频率
	chromAttenuation = 0.1;
	delta = 0;
	exaggeration_factor = 2.0;
	lambda = 0;
}

/**
* getFrameRate	-	return the frame rate
*
*
* @return the frame rate
*/
double VideoProcessor::getFrameRate()
{
	double r = capture.get(CV_CAP_PROP_FPS);

	return r;
}

/**
* getLengthMS	-	return the video length in milliseconds
*
*
* @return the length of length in milliseconds
*/
double VideoProcessor::getLengthMS()
{
	double l = 1000.0 * length / rate;
	return l;
}

/**
* spatialFilter	-	spatial filtering an image
*
* @param src		-	source image
* @param pyramid	-	destinate pyramid
*/
bool VideoProcessor::spatialFilter(const cv::Mat &src, std::vector<cv::Mat> &pyramid)
{
	switch (spatialType) {
		case LAPLACIAN:     // laplacian pyramid
			return buildLaplacianPyramid(src, levels, pyramid);
			break;
		case GAUSSIAN:      // gaussian pyramid
			return buildGaussianPyramid(src, levels, pyramid);
			break;
		default:
			return false;
			break;
	}
}

/**
* temporalFilter	-	temporal filtering an image
*
* @param src	-	source image
* @param dst	-	destinate image
*/
void VideoProcessor::temporalFilter(const cv::Mat &src,
									cv::Mat &dst)
{
	switch (temporalType) {
		case IIR:       // IIR bandpass filter
			temporalIIRFilter(src, dst);
			break;
		case IDEAL:     // Ideal bandpass filter
			temporalIdealFilter(src, dst);
			break;
		default:
			break;
	}
	return;
}

/**
* temporalIIRFilter	-	temporal IIR filtering an image
*                          (thanks to Yusuke Tomoto)
* @param pyramid	-	source image
* @param filtered	-	filtered result
*
*/
void VideoProcessor::temporalIIRFilter(const cv::Mat &src,
									   cv::Mat &dst)
{
	cv::Mat temp1 = (1 - fh)*lowpass1[curLevel] + fh*src;
	cv::Mat temp2 = (1 - fl)*lowpass2[curLevel] + fl*src;
	lowpass1[curLevel] = temp1;
	lowpass2[curLevel] = temp2;
	dst = lowpass1[curLevel] - lowpass2[curLevel];
}

/**
* temporalIdalFilter	-	temporal IIR filtering an image pyramid of concat-frames
*                          (Thanks to Daniel Ron & Alessandro Gentilini)
*
* @param pyramid	-	source pyramid of concatenate frames
* @param filtered	-	concatenate filtered result
*
*/
void VideoProcessor::temporalIdealFilter(const cv::Mat &src,
										 cv::Mat &dst)
{
	cv::Mat channels[3];

	// split into 3 channels
	cv::split(src, channels);

	for (int i = 0; i < 3; ++i){

		cv::Mat current = channels[i];  // current channel
		cv::Mat tempImg;

		int width = cv::getOptimalDFTSize(current.cols);
		int height = cv::getOptimalDFTSize(current.rows);

		cv::copyMakeBorder(current, tempImg,
						   0, height - current.rows,
						   0, width - current.cols,
						   cv::BORDER_CONSTANT, cv::Scalar::all(0));

		// do the DFT
		cv::dft(tempImg, tempImg, cv::DFT_ROWS | cv::DFT_SCALE);

		// construct the filter
		cv::Mat filter = tempImg.clone();
		createIdealBandpassFilter(filter, fl, fh, rate);

		// apply filter
		cv::mulSpectrums(tempImg, filter, tempImg, cv::DFT_ROWS);

		// do the inverse DFT on filtered image
		cv::idft(tempImg, tempImg, cv::DFT_ROWS | cv::DFT_SCALE);

		// copy back to the current channel
		tempImg(cv::Rect(0, 0, current.cols, current.rows)).copyTo(channels[i]);
	}
	// merge channels
	cv::merge(channels, 3, dst);

	// normalize the filtered image
	cv::normalize(dst, dst, 0, 1, CV_MINMAX);
}

/**
* amplify	-	ampilfy the motion
*
* @param filtered	- motion image
*/
void VideoProcessor::amplify(const cv::Mat &src, cv::Mat &dst)
{
	float currAlpha;
	switch (spatialType) {
		case LAPLACIAN:
			//compute modified alpha for this level
			currAlpha = lambda / delta / 8 - 1;
			currAlpha *= exaggeration_factor;
			if (curLevel == levels || curLevel == 0)     // ignore the highest and lowest frequency band
				dst = src * 0;
			else
				dst = src * cv::min(alpha, currAlpha);
			break;
		case GAUSSIAN:
			dst = src * alpha;
			break;
		default:
			break;
	}
}

/**
* concat	-	concat all the frames into a single large Mat
*              where each column is a reshaped single frame
*
* @param frames	-	frames of the video sequence
* @param dst		-	destinate concatnate image
*/
void VideoProcessor::concat(const std::vector<cv::Mat> &frames,
							cv::Mat &dst)
{
	cv::Size frameSize = frames.at(0).size();
	cv::Mat temp(frameSize.width*frameSize.height, length - 1, CV_32FC3);
	for (int i = 0; i < length - 1; ++i) {
		// get a frame if any
		cv::Mat input = frames.at(i);
		// reshape the frame into one column
		// 像素总数不变，但row变成总数，意味着column为1
		cv::Mat reshaped = input.reshape(3, input.cols*input.rows).clone();
		cv::Mat line = temp.col(i);
		// save the reshaped frame to one column of the destinate big image
		reshaped.copyTo(line);
	}
	temp.copyTo(dst);
	std::cout << "ok";
}

/**
* deConcat	-	de-concat the concatnate image into frames
*
* @param src       -   source concatnate image
* @param framesize	-	frame size
* @param frames	-	destinate frames
*/
void VideoProcessor::deConcat(const cv::Mat &src,
							  const cv::Size &frameSize,
							  std::vector<cv::Mat> &frames)
{
	for (int i = 0; i < length - 1; ++i) {    // get a line if any
		cv::Mat line = src.col(i).clone();
		cv::Mat reshaped = line.reshape(3, frameSize.height).clone();
		frames.push_back(reshaped);
	}
}

/**
* createIdealBandpassFilter	-	create a 1D ideal band-pass filter
*
* @param filter    -	destinate filter
* @param fl        -	low cut-off
* @param fh		-	high cut-off
* @param rate      -   sampling rate(i.e. video frame rate)
*/
void VideoProcessor::createIdealBandpassFilter(cv::Mat &filter, double fl, double fh, double rate)
{
	int width = filter.cols;
	int height = filter.rows;

	fl = 2 * fl * width / rate;
	fh = 2 * fh * width / rate;

	double response;

	for (int i = 0; i < height; ++i) {
		for (int j = 0; j < width; ++j) {
			// filter response
			if (j >= fl && j <= fh)
				response = 1.0f;
			else
				response = 0.0f;
			filter.at<float>(i, j) = response;
		}
	}
}

/**
* setInput	-	set the name of the expected video file
*
* @param fileName	-	the name of the video file
*
* @return True if success. False otherwise
*/
bool VideoProcessor::setInput(const std::string &fileName)
{
	fnumber = 0;
	tempFile = fileName;

	// In case a resource was already
	// associated with the VideoCapture instance
	if (isOpened()){
		capture.release();
	}

	// Open the video file
	if (capture.open(fileName)){
		// read parameters
		length = capture.get(CV_CAP_PROP_FRAME_COUNT);
		rate = getFrameRate();
		//cv::Mat input;
		// show first frame
		//getNextFrame(input);
		//emit showFrame(input);
		//emit updateBtn();
		return true;
	}
	else {
		return false;
	}
}

/**
* setSpatialFilter	-	set the spatial filter
*
* @param type	-	spatial filter type. Could be:
*					1. LAPLACIAN: laplacian pyramid
*					2. GAUSSIAN: gaussian pyramid
*/
void VideoProcessor::setSpatialFilter(spatialFilterType type)
{
	spatialType = type;
}

/**
* setTemporalFilter	-	set the temporal filter
*
* @param type	-	temporal filter type. Could be:
*					1. IIR: second order(IIR) filter
*					2. IDEAL: ideal bandpass filter
*/
void VideoProcessor::setTemporalFilter(temporalFilterType type)
{
	temporalType = type;
}

/**
* close	-	close the video
*
*/
void VideoProcessor::close()
{
	rate = 0;
	length = 0;
	modify = 0;
	capture.release();
	writer.release();
	tempWriter.release();
}


/**
* isStop	-	Is the processing stop
*
*
* @return True if not processing/playing. False otherwise
*/
bool VideoProcessor::isStop()
{
	return stop;
}

/**
* isOpened	-	Is the player opened?
*
*
* @return True if opened. False otherwise
*/
bool VideoProcessor::isOpened()
{
	return capture.isOpened();
}

/**
* getNextFrame	-	get the next frame if any
*
* @param frame	-	the expected frame
*
* @return True if success. False otherwise
*/
bool VideoProcessor::getNextFrame(cv::Mat &frame)
{
	return capture.read(frame);
}

/**
* findPeaks	-	find peaks in the curve
*
*/
int findPeaks(std::vector<double> smoothedData) {
	double diff;
	std::vector<int> sign;
	for (int i = 1; i < smoothedData.size(); ++i) {
		diff = smoothedData[i] - smoothedData[i - 1];
		if (diff > 0) {
			sign.push_back(1);
		}
		else if (diff < 0) {
			sign.push_back(-1);
		}
		else {
			sign.push_back(0);
		}
	}
	int peaks = 0;
	for (int j = 1; j < sign.size(); j++) {
		diff = sign[j] - sign[j - 1];
		if (diff < 0) {
			peaks++;
		}
	}
	return peaks;
}

/**
* smooth   - 	smooth the curve with Moving Average Filtering
*
*/
void smooth(std::vector<double> input_data, unsigned int len, std::vector<double> & output_data, unsigned int span) {
	unsigned int i = 0, j = 0;
	unsigned int pn = 0, n = 0;
	double sum = 0.0;

	if (span % 2 == 1) {
		n = (span - 1) / 2;
	}
	else{
		n = (span - 2) / 2;
	}

	for (i = 0; i < len; ++i) {
		pn = n;

		if (i < n) {
			pn = i;
		}
		else if ((len - 1 - i) < n) {
			pn = len - i - 1;
		}

		sum = 0.0;
		for (j = i - pn; j <= i + pn; ++j) {
			sum += input_data[j];
		}
		output_data.push_back(sum / (pn * 2 + 1));
	}
}

/**
* colorMagnify	-	color magnification
*
*/
int VideoProcessor::colorMagnify()
{
	// set filter
	setSpatialFilter(GAUSSIAN);
	setTemporalFilter(IDEAL);

	// create a temp file
	// createTemp();

	// current frame
	cv::Mat input;
	// output frame
	cv::Mat output;
	// motion image

	cv::Mat motion;
	// temp image
	cv::Mat temp;

	// video frames
	std::vector<cv::Mat> frames;
	// down-sampled frames
	std::vector<cv::Mat> downSampledFrames;
	// filtered frames
	std::vector<cv::Mat> filteredFrames;

	// concatenate image of all the down-sample frames
	cv::Mat videoMat;
	// concatenate filtered image
	cv::Mat filtered;

	// if no capture device has been set
	if (!isOpened())
		return 0;

	// set the modify flag to be true
	modify = true;

	// is processing
	stop = false;

	// 1. spatial filtering
	while (getNextFrame(input) && !isStop()) {
		input.convertTo(temp, CV_32FC3);
		frames.push_back(temp.clone());

		// spatial filtering
		std::vector<cv::Mat> pyramid;
		spatialFilter(temp, pyramid);
		downSampledFrames.push_back(pyramid.at(levels - 1));
	}

	// 2. concat all the frames into a single large Mat
	// where each column is a reshaped single frame
	// (for processing convenience)
	concat(downSampledFrames, videoMat);

	// 3. temporal filtering
	temporalFilter(videoMat, filtered);

	// 4. amplify color motion
	amplify(filtered, filtered);

	// 5. de-concat the filtered image into filtered frames
	deConcat(filtered, downSampledFrames.at(0).size(), filteredFrames);

	cv::Mat cl[3];
	std::vector<double> markdata;

	// 6. amplify each frame
	// by adding frame image and motions
	// and write into video
	fnumber = 0;
	for (int i = 0; i<length - 1 && !isStop(); ++i) {
		// up-sample the motion image
		upsamplingFromGaussianPyramid(filteredFrames.at(i), levels, motion);
		resize(motion, motion, frames.at(i).size());
		temp = frames.at(i) + motion;
		output = temp.clone();

		double minVal, maxVal;
		minMaxLoc(output, &minVal, &maxVal); //find minimum and maximum intensities

		output.convertTo(output, CV_8UC3, 255.0 / (maxVal - minVal),
						 -minVal * 255.0 / (maxVal - minVal));

		cv::split(output, cl);
		cv::Scalar mean = cv::mean(cl[1]);
		markdata.push_back(mean[0]);
	}

	// Smooth the curve
	std::vector<double> smoothedData;
	smooth(markdata, markdata.size(), smoothedData, 11);

	// Find Peaks
	return findPeaks(smoothedData);
}
