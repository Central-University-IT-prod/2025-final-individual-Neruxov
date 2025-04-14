package xyz.neruxov.advertee.util

import org.springframework.stereotype.Component
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO


/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@Component
class ImageResizer {

    fun resizeTo(image: ByteArray, maxSide: Int): ByteArray {
        val inputImage = ImageIO.read(image.inputStream())

        val originalWidth = inputImage.width
        val originalHeight = inputImage.height

        if (originalWidth <= maxSide && originalHeight <= maxSide) {
            return image
        }

        val width: Int
        val height: Int

        if (originalWidth < originalHeight) {
            height = maxSide
            width = (originalWidth * height) / originalHeight
        } else {
            width = maxSide
            height = (originalHeight * width) / originalWidth
        }

        val resizedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g2d = resizedImage.createGraphics()

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g2d.drawImage(inputImage, 0, 0, width, height, null)
        g2d.dispose()

        val outputStream = ByteArrayOutputStream()
        ImageIO.write(resizedImage, "png", outputStream)

        return outputStream.toByteArray()
    }

}