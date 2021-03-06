/*
* Copyright (C) 2007-2008 Gilles Gigan (gilles.gigan@gmail.com)
* eResearch Centre, James Cook University (eresearch.jcu.edu.au)
*
* This program was developed as part of the ARCHER project
* (Australian Research Enabling Environment) funded by a   
* Systemic Infrastructure Initiative (SII) grant and supported by the Australian
* Department of Innovation, Industry, Science and Research
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public  License as published by the
* Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
* or FITNESS FOR A PARTICULAR PURPOSE.  
* See the GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
*
*/

package au.edu.jcu.v4l4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import au.edu.jcu.v4l4j.exceptions.UnsupportedMethod;

/**
 * This class encapsulates information about the supported capture resolutions
 * and frame intervals for a given {@link ImageFormat}. The first step is to
 * determine how the supported resolutions are expressed by calling
 * {@link #getType()}. This method returns either:
 * <ul>
 * <li>{@link Type#UNSUPPORTED}: no resolution information can be obtained from
 * the driver. Calling any methods (except {@link #getType()}) will throw a
 * {@link UnsupportedMethod} exception.</li>
 * <li>{@link Type#DISCRETE}: supported resolutions and frame intervals are
 * returned as a list of {@link DiscreteResolution} objects, which encapsulate
 * the width and height for this resolution as well as supported frame
 * intervals. Calling {@link #getDiscreteResolutions()} returns this list.</li>
 * <li>{@link Type#STEPWISE}: supported width and height values can be anywhere
 * within a given minimum and maximum using a step value. These values, along
 * with information on supported frame intervals are encapsulated in a
 * {@link StepwiseResolution} object which is retrieved by calling
 * {@link #getStepwiseResolution()}.</li>
 * </ul>
 * {@link StepwiseResolution} and {@link DiscreteResolution} objects provide
 * information on supported capture resolutions and frame intervals for a given
 * image format.<br>
 * {@link ResolutionInfo} objects are not instantiated directly. Instead, each
 * {@link ImageFormat} carries a reference to a {@link ResolutionInfo} object,
 * describing the supported resolutions for this format. A list of supported
 * image formats is obtained by calling {@link DeviceInfo#getFormatList()} on a
 * {@link VideoDevice}. See the {@link ImageFormatList} documentation for more
 * information.
 * 
 * @author gilles
 *
 */
public class ResolutionInfo {
	/**
	 * The Type enumeration defines how the supported resolutions are expressed.
	 * If DISCRETE, then supported resolutions are reported as a list of
	 * {@link DiscreteResolution} objects. If STEPWISE, then minimum, maximum
	 * and step values for width and height are reported. If UNSUPPORTED, then
	 * resolution information is not available.
	 * 
	 * @author gilles
	 *
	 */
	public static enum Type {
		/**
		 * An UNSUPPORTED type means supported resolution information cannot be
		 * obtained from the driver. Calling any methods on the
		 * {@link ResolutionInfo} object (except
		 * {@link ResolutionInfo#getType()} will throw an
		 * {@link UnsupportedMethod} exception.
		 */
		UNSUPPORTED,
		/**
		 * A DISCRETE type means resolutions are reported as a list of
		 * {@link DiscreteResolution} objects, using
		 * {@link ResolutionInfo#getDiscreteResolutions()}
		 */
		DISCRETE,
		/**
		 * A STEPWISE type means that resolutions are reported as minimum,
		 * maximum and step values for width and height through a
		 * {@link StepwiseResolution} object. This object can be obtained by
		 * calling {@link ResolutionInfo#getStepwiseResolution()}.
		 */
		STEPWISE
	};

	/**
	 * The resolution information type
	 */
	private final Type type;

	/**
	 * The stepwise resolution object Valid only if type==STEPWISE
	 */
	private final StepwiseResolution stepwiseObject;

	/**
	 * A list of {@link DiscreteResolution} object if type==DISCRETE
	 */
	private final List<DiscreteResolution> discreteValues;

	/**
	 * This native method returns the type of the supported resolutions.
	 * 
	 * @param o
	 *            a C pointer to a struct v4l4j_device
	 * @return 0: unsupported, 1: discrete, 2: continuous
	 */
	private static final native int doGetType(int index, long o);

	/**
	 * This native method sets the stepwise attributes (min, max & step width &
	 * height)
	 * 
	 * @param index
	 *            the image format index
	 * @param o
	 *            a C pointer to a struct v4l4j_device
	 */
	private static final native StepwiseResolution doGetStepwise(int index, long o);
	
	/**
	 * This native method sets the discrete resolution list (discreteValues)
	 * 
	 * @param index
	 *            the image format index
	 * @param o
	 *            a C pointer to a struct v4l4j_device
	 */
	private static final native void doGetDiscrete(List<DiscreteResolution> discreteValues, int index, long o);

	/**
	 * This method builds a new resolution information object. It MUST be called
	 * while the device info interface of libvideo is acquired.
	 * 
	 * @param index
	 *            the libvideo index of the image format for which this
	 *            resolution info object is to be constructed
	 * @param object
	 *            a C pointer to a struct v4l4j_device
	 */
	ResolutionInfo(int index, long object) {
		Type type = Type.UNSUPPORTED;
		StepwiseResolution stepwise = null;
		ArrayList<DiscreteResolution> discrete = null;
		try {
			switch (doGetType(index, object)) {
				case 1:
					type = Type.DISCRETE;
					discrete = new ArrayList<DiscreteResolution>();
					doGetDiscrete(discrete, index, object);
					discrete.trimToSize();
					break;
				case 2:
					type = Type.STEPWISE;
					stepwise = doGetStepwise(index, object);
					break;
				case 0:
				default:
					type = Type.UNSUPPORTED;
					break;
			}
		} catch (Exception e) {
			// error checking supported resolutions
			e.printStackTrace();
			System.err.println("There was an error checking the supported resolutions.\n" + V4L4JConstants.REPORT_ERROR_MSG);
			type = Type.UNSUPPORTED;
		} finally {
			this.type = type;
			this.stepwiseObject = stepwise;
			this.discreteValues = discrete;
		}
	}

	/**
	 * This method returns the resolution information type. See {@link Type}
	 * enumeration.
	 * 
	 * @return the resolution information type. See {@link Type}.
	 */
	public Type getType() {
		return type;
	}

	/**
	 * This method returns a list of {@link DiscreteResolution}s, or throws a
	 * {@link UnsupportedMethod} exception if this resolution info object is not
	 * of type {@link Type#DISCRETE}.
	 * 
	 * @return a list of {@link DiscreteResolution}s
	 * @throws UnsupportedMethod
	 *             if this resolution info object is not of type
	 *             {@link Type#DISCRETE}.
	 */
	public List<DiscreteResolution> getDiscreteResolutions() throws UnsupportedMethod {
		if (type != Type.DISCRETE)
			throw new UnsupportedMethod("Supported resolutions are not discrete");
		return Collections.unmodifiableList(this.discreteValues);
	}

	/**
	 * This method returns a {@link StepwiseResolution} object, or throws a
	 * {@link UnsupportedMethod} exception if this resolution info object is not
	 * of type {@link Type#STEPWISE}.
	 * 
	 * @return a {@link StepwiseResolution} object
	 * @throws UnsupportedMethod
	 *             if this resolution info object is not of type
	 *             {@link Type#STEPWISE}.
	 */
	public StepwiseResolution getStepwiseResolution() throws UnsupportedMethod {
		if (type != Type.STEPWISE)
			throw new UnsupportedMethod("Supported resolutions are not stepwsie");
		return stepwiseObject;
	}

	@Override
	public String toString() {
		if (type == Type.STEPWISE) {
			return stepwiseObject.toString();
		} else if (type == Type.DISCRETE) {
			StringBuilder sb = new StringBuilder();
			for (DiscreteResolution d : discreteValues)
				sb.append(d).append(" - ");
			if (sb.length() > 3)
				sb.setLength(sb.length() - 3);
			else
				sb.append("[no resolutions]");
			return sb.toString();
		} else {
			return "no resolution information";
		}
	}

	/**
	 * This class represents a possible resolution supported by a video device.
	 * It also contains information on supported frame intervals at this
	 * resolution, encapsulated in a {@link FrameInterval} object returned by
	 * the {@link #getFrameInterval()} method.<br>
	 * A {@link DiscreteResolution} object is not instantiated directly.
	 * Instead, a list of supported {@link DiscreteResolution}s for a given
	 * video device can be obtained through a {@link ResolutionInfo} for that
	 * device.
	 * 
	 * @author gilles
	 */
	public static final class DiscreteResolution {

		/**
		 * The resolution width
		 */
		private final int width;

		/**
		 * The resolution height
		 */
		private final int height;

		/**
		 * The frame interval object containing information on supported frame
		 * intervals for capture at this resolution.
		 */
		private final FrameInterval interval;

		private DiscreteResolution(int width, int height, FrameInterval interval) {
			this.width = width;
			this.height = height;
			this.interval = interval;
		}

		/**
		 * This method returns the resolution width
		 * 
		 * @return the resolution width
		 */
		public int getWidth() {
			return this.width;
		}

		/**
		 * This method returns the resolution height
		 * 
		 * @return the resolution height
		 */
		public int getHeight() {
			return this.height;
		}

		/**
		 * This method returns the frame interval object, containing information
		 * on supported frame intervals for this discrete resolution.
		 * 
		 * @return the frame intervals supported at this resolution
		 */
		public FrameInterval getFrameInterval() {
			return this.interval;
		}

		@Override
		public String toString() {
			return new StringBuilder()
					.append(width).append('x').append(height)
					.append(" (").append(interval).append(')')
					.toString();
		}
	}

	/**
	 * <p>
	 * This class encapsulates information about supported capture resolutions
	 * for a video device. The supported resolutions are continuous values,
	 * comprised between a minimum and a maximum, in given increments (called
	 * the step value).
	 * </p>
	 * <p>
	 * For instance, for a device supporting capture resolutions between 160x120
	 * and 800x600 in increments of 160x120, the following resolutions are
	 * supported: 160x120, 320x240, 480x360, 640x480, 800x600.
	 * </p>
	 * <p>
	 * A <code>StepwiseResolution<code> object matching the above criteria will
	 * contain:
	 * <ul>
	 * <li><code>StepwiseResolution.minWidth = 160</code></li>
	 * <li><code>StepwiseResolution.minHeight = 120</code></li>
	 * <li><code>StepwiseResolution.stepWidth = 160</code></li>
	 * <li><code>StepwiseResolution.stepHeight= 120</code></li>
	 * <li><code>StepwiseResolution.maxWidth = 800</code></li>
	 * <li><code>StepwiseResolution.maxHeight = 600</code></li>
	 * </ul>
	 * These values can also be obtained using the accessor methods.
	 * </p>
	 * <p>
	 * Objects of this class also contains two {@link FrameInterval} objects
	 * providing information on supported frame intervals for capture at the
	 * minimum resolution ({@link StepwiseResolution#getMinResFrameInterval()})
	 * and maximum resolution (
	 * {@link StepwiseResolution#getMaxResFrameInterval()}). You can find out
	 * supported frame intervals for any other intermediate resolution by
	 * calling {@link DeviceInfo#listIntervals(ImageFormat, int, int)}. A
	 * StepwiseResolution object is not directly instantiated. Instead, it can
	 * be obtained through a {@link ResolutionInfo}.
	 * </p>
	 * 
	 * @author gilles
	 */
	public static class StepwiseResolution {
		/**
		 * The minimum, maximum and step values for both width and height
		 */
		public final int minWidth, maxWidth, stepWidth, minHeight, maxHeight, stepHeight;

		/**
		 * The frame interval object containing information on supported frame
		 * intervals for capture at the minimum resolution (minWidth x
		 * minHeight).
		 */
		private final FrameInterval minInterval;

		/**
		 * The frame interval object containing information on supported frame
		 * intervals for capture at the maximum resolution (maxWidth x
		 * maxHeight).
		 */
		private final FrameInterval maxInterval;

		StepwiseResolution(int minWidth, int minHeight, int maxWidth, int maxHeight, int stepWidth, int stepHeight,
				FrameInterval minInterval, FrameInterval maxInterval) {
			this.minWidth = minWidth;
			this.maxWidth = maxWidth;
			this.stepWidth = stepWidth;
			this.stepHeight = stepHeight;
			this.minHeight = minHeight;
			this.maxHeight = maxHeight;
			this.minInterval = minInterval;
			this.maxInterval = maxInterval;
		}

		/**
		 * This method returns the minimum width.
		 * 
		 * @return the minimum width
		 */
		public int getMinWidth() {
			return minWidth;
		}

		/**
		 * This method returns the maximum width.
		 * 
		 * @return the maximum width
		 */
		public int getMaxWidth() {
			return maxWidth;
		}

		/**
		 * This method returns the width step.
		 * 
		 * @return the width step
		 */
		public int getWidthStep() {
			return stepWidth;
		}

		/**
		 * This method returns the minimum height.
		 * 
		 * @return the minimum height
		 */
		public int getMinHeight() {
			return minHeight;
		}

		/**
		 * This method returns the maximum height.
		 * 
		 * @return the maximum height
		 */
		public int getMaxHeight() {
			return maxHeight;
		}

		/**
		 * This method returns the height step.
		 * 
		 * @return the height step
		 */
		public int getHeightStep() {
			return stepHeight;
		}

		/**
		 * This method returns the frame interval object, containing information
		 * on all supported frame interval at the minimum resolution (minWidth x
		 * minHeight)
		 * 
		 * @return the frame interval supported at the minimum resolution
		 *         (minWidth x minHeight)
		 */
		public FrameInterval getMinResFrameInterval() {
			return minInterval;
		}

		/**
		 * This method returns the frame interval object, containing information
		 * on all supported frame interval at the maximum resolution (maxWidth x
		 * maxHeight)
		 * 
		 * @return the frame interval supported at the maximum resolution
		 *         (maxWidth x maxHeight)
		 */
		public FrameInterval getMaxResFrameInterval() {
			return maxInterval;
		}

		@Override
		public String toString() {
			//TODO find some good initial size
			return new StringBuilder()
					.append("min: ").append(this.getMinWidth()).append('x').append(this.getMinHeight())
					.append(" (").append(this.getMinResFrameInterval()).append(") - max: ")
					.append(this.getMaxWidth()).append('x').append(this.getMaxHeight())
					.append(" (").append(this.getMaxResFrameInterval()).append(") - step: ")
					.append(this.getWidthStep()).append('x').append(this.getHeightStep())
					.append(')')
					.toString();
		}
	}
}
