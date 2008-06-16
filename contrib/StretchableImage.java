import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import pulpcore.image.CoreImage;
import pulpcore.image.CoreGraphics;

/**
 * Stretchable images are images that, when scaled, keep the corners static and scale the inner 
 * sections. The format used is Android's nine-patch image:
 * http://code.google.com/android/reference/available-resources.html#ninepatch
 *
 * Example:
 * <pre>
 * import pulpcore.image.Colors;
 * import pulpcore.scene.Scene2D;
 * import pulpcore.sprite.FilledSprite;
 * import pulpcore.sprite.ImageSprite;
 * 
 * public class StretchableImageTest extends Scene2D {
 *     @Override
 *     public void load() {
 *         StretchableImage image = StretchableImage.load("button.9.png");
 *         add(new FilledSprite(Colors.BLUE));
 *         add(new ImageSprite(image, 5, 5));
 *         add(new ImageSprite(image.scale(200, image.getHeight()), 5, 32));
 *         add(new ImageSprite(image.scale(200, 100), 5, 59));
 *     }
 * }
 * </pre>
 * Due to a limitation in PulpCore's software renderer, the sides and center are scaled with
 * nearest-neighbor interpolation. Future versions of the software renderer will allow
 * bilinear interpolation. 
 *
 * @author Christoffer Lerno
 * @version $Revision$ $Date$   $Author$
 */
public class StretchableImage extends CoreImage
{

	private final CoreImage[][] m_patches;
	private final Sections m_hSections;
	private final Sections m_vSections;
	private final Sections m_hPad;
	private final Sections m_vPad;
	private int m_topPad;
	private int m_bottomPad;
	private int m_leftPad;
	private int m_rightPad;

	private StretchableImage(CoreImage coreImage,
	                        CoreImage[][] patches,
	                        Sections hSections,
	                        Sections vSections,
	                        Sections hPadSections,
	                        Sections vPadSections)
	{
		super(coreImage);
		m_patches = patches;
		m_hSections = hSections;
		m_vSections = vSections;
		m_hPad = hPadSections;
		m_vPad = vPadSections;
		Sections scaledHorizontalPad = hPadSections.scale(getWidth());
		Sections scaledVerticalPad = vPadSections.scale(getHeight());
		m_leftPad = scaledHorizontalPad.firstPad();
		m_rightPad = scaledHorizontalPad.lastPad();
		m_topPad = scaledVerticalPad.firstPad();
		m_bottomPad = scaledVerticalPad.lastPad();
	}

	public int getBottomPad()
	{
		return m_bottomPad;
	}

	public int getLeftPad()
	{
		return m_leftPad;
	}

	public int getRightPad()
	{
		return m_rightPad;
	}

	public int getTopPad()
	{
		return m_topPad;
	}

	public StretchableImage scale(int width, int height)
	{
		CoreImage scaledImage = new CoreImage(width, height, false);
		CoreGraphics g = scaledImage.createGraphics();
		g.setInterpolation(CoreGraphics.INTERPOLATION_NEAREST_NEIGHBOR);
		int xParts = m_hSections.getParts();
		int yParts = m_vSections.getParts();
		Sections hScaled = m_hSections.scale(width);
		Sections vScaled = m_vSections.scale(height);
		for (int x = 0; x < xParts; x++)
		{
			Section xSection = hScaled.get(x);
			for (int y = 0; y < yParts; y++)
			{
				Section ySection = vScaled.get(y);
				g.drawScaledImage(m_patches[x][y],
				                  xSection.getPos(),
				                  ySection.getPos(),
				                  xSection.getSize(),
				                  ySection.getSize());
			}
		}
		return new StretchableImage(scaledImage, m_patches, m_hSections, m_vSections, m_hPad, m_vPad);
	}

	private static enum NineWaySection
	{
		HORIZONTAL,
		VERTICAL,
		HORIZONTAL_PAD,
		VERTICAL_PAD,
	}
    
    public static StretchableImage load(String imageName)
    {
        return load(CoreImage.load(imageName));
    }

	public static StretchableImage load(CoreImage nineWayStretchImage)
	{
		Sections vSections = findSections(nineWayStretchImage, NineWaySection.VERTICAL);
		Sections hSections = findSections(nineWayStretchImage, NineWaySection.HORIZONTAL);
		Sections vPadSections = findSections(nineWayStretchImage, NineWaySection.VERTICAL_PAD);
		Sections hPadSections = findSections(nineWayStretchImage, NineWaySection.HORIZONTAL_PAD);
		int xParts = hSections.getParts();
		int yParts = vSections.getParts();
		CoreImage[][] splitImage = new CoreImage[xParts][yParts];
		for (int x = 0; x < xParts; x++)
		{
			Section xSection = hSections.get(x);
			for (int y = 0; y <  yParts; y++)
			{
				Section ySection = vSections.get(y);
				splitImage[x][y] = nineWayStretchImage.crop(xSection.getPos() + 1, ySection.getPos() + 1,
			                                                xSection.getSize(), ySection.getSize());
			}
		}
		return new StretchableImage(nineWayStretchImage.crop(1, 1,
		                                                     nineWayStretchImage.getWidth() - 2,
		                                                     nineWayStretchImage.getHeight() - 2),
		                            splitImage,
	                                hSections,
	                                vSections,
	                                hPadSections,
	                                vPadSections);
	}

	private static Sections findSections(CoreImage nineWayStretchImage, NineWaySection sectionEnum)
	{
		int size;
		switch (sectionEnum)
		{
			case HORIZONTAL:
			case HORIZONTAL_PAD:
				size = nineWayStretchImage.getWidth();
				break;
			default:
				size = nineWayStretchImage.getHeight();
		}
		List<Section> sections = new ArrayList<Section>();
		int currentSectionStart = 1;
		boolean currentTransparency = true;
		for (int i = 1; i < size; i++)
		{
			boolean isTransparent = currentTransparency;
			switch (sectionEnum)
			{
				case HORIZONTAL:
					isTransparent = nineWayStretchImage.isTransparent(i, 0);
					break;
				case HORIZONTAL_PAD:
					isTransparent = nineWayStretchImage.isTransparent(i, nineWayStretchImage.getHeight() - 1);
					break;
				case VERTICAL:
					isTransparent = nineWayStretchImage.isTransparent(0, i);
					break;
				case VERTICAL_PAD:
					isTransparent = nineWayStretchImage.isTransparent(nineWayStretchImage.getWidth() - 1, i);
					break;
			}
			if (isTransparent != currentTransparency || i == size - 1)
			{
				if (i != 1)
				{
					sections.add(new Section(sections.size(),
					                         currentSectionStart - 1,
					                         i - currentSectionStart,
					                         !currentTransparency));
					currentSectionStart = i;
				}
			}
			currentTransparency = isTransparent;
		}
		return new Sections(sections);
	}

	private static class Sections
	{
		private int m_flexLength;
		private int m_fixedLength;
		private List<Section> m_sections;

		private Sections(List<Section> sections)
		{
			m_flexLength = 0;
			m_fixedLength = 0;
			for (Section section : sections)
			{
				if (section.isStretchable())
				{
					m_flexLength += section.getSize();
				}
				else
				{
					m_fixedLength += section.getSize();
				}
			}
			m_sections = sections;
		}

		public int getTotalLength()
		{
			return m_flexLength + m_fixedLength;
		}

		public int getFlexLength()
		{
			return m_flexLength;
		}

		public int getParts()
		{
			return m_sections.size();
		}

		public int getFixedLength()
		{
			return m_fixedLength;
		}

		public Sections scale(int newSize)
		{
			int flexPartLength = newSize - m_fixedLength;
			int fixedPartLength = m_fixedLength;
			if (flexPartLength < 0)
			{
				m_fixedLength += flexPartLength;
				flexPartLength = 0;
			}
			double flexScaleFactor = flexPartLength / (double) m_flexLength;
			double fixedScaleFactor = fixedPartLength / (double) m_fixedLength;
			Integer[] scaledLengths = new Integer[m_sections.size()];
			int length = 0;
			for (int i = 0; i < scaledLengths.length; i++)
			{
				scaledLengths[i] = (int) (m_sections.get(i).getSize()
				                          * (m_sections.get(i).isStretchable()
				                             ? flexScaleFactor
				                             : fixedScaleFactor));
				length += scaledLengths[i];
			}
			LinkedList<Section> sorted = new LinkedList<Section>(m_sections);
			Collections.sort(sorted);
			boolean addToStrechable = fixedPartLength == m_fixedLength;
			while (length < newSize)
			{
				Section section = sorted.removeLast();
				if (section.isStretchable() == addToStrechable)
				{
					scaledLengths[section.getOrder()]++;
					length++;
				}
			}
			List<Section> sections = new ArrayList<Section>(scaledLengths.length);
			int x = 0;
			for (int i = 0; i < scaledLengths.length; i++)
			{
				sections.add(new Section(i, x, scaledLengths[i], m_sections.get(i).isStretchable()));
				x += scaledLengths[i];
			}
			return new Sections(sections);
		}

		public Section get(int i)
		{
			return m_sections.get(i);
		}

		public int firstPad()
		{
			return m_sections.size() < 1 || m_sections.get(0).isStretchable() ? 0 : m_sections.get(1).getPos();
		}

		public int lastPad()
		{
			return m_sections.size() < 1 || m_sections.get(getParts() - 1).isStretchable()
			       ? m_fixedLength + m_flexLength
		           : m_sections.get(getParts() - 1).getPos();
		}
	}

	private static class Section implements Comparable<Section>
	{
		private final int m_pos;
		private final int m_size;
		private final boolean m_stretchable;
		private final int m_order;

		public Section(int order, int position, int size, boolean stretchable)
		{
			m_order = order;
			m_pos = position;
			m_size = size;
			m_stretchable = stretchable;
		}

		public int getOrder()
		{
			return m_order;
		}

		public int getPos()
		{
			return m_pos;
		}

		public int getSize()
		{
			return m_size;
		}

		public boolean isStretchable()
		{
			return m_stretchable;
		}

		public int compareTo(Section o)
		{
			return m_size - o.m_size;
		}
	}
}
