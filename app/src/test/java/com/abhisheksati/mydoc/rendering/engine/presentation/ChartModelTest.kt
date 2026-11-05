package com.abhisheksati.mydoc.rendering.engine.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

/**
 * The chart parts a presentation carries instead of drawing: the values live in the part itself, so
 * the reader has to take them from `c:numCache` rather than from the workbook the chart was built
 * from.
 *
 * These fixtures are written the way PowerPoint writes them - `c:pt` with its own index, series
 * names in a one-point string cache, categories shared by the series of a group.
 */
class ChartModelTest {

    private val c = "xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\" "
    private val a = "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" "
    private val r = "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\""

    private fun parse(xml: String): ChartModel.Chart? =
        ChartModel.parse(OoxmlNode.parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8))))

    private fun bar(dir: String = "col") = """
        <c:chartSpace ${c}${a}${r}>
          <c:chart>
            <c:title><a:tx><a:rich><a:p><a:r><a:t>Units per quarter</a:t></a:r></a:p></a:rich></a:tx></c:title>
            <c:plotArea>
              <c:barChart>
                <c:barDir val="$dir"/>
                <c:ser><c:idx val="0"/>
                  <c:tx><c:strRef><c:strCache><c:ptCount val="1"/><c:pt idx="0"><c:v>North</c:v></c:pt></c:strCache></c:strRef></c:tx>
                  <c:cat><c:strRef><c:strCache><c:ptCount val="3"/>
                    <c:pt idx="0"><c:v>Q1</c:v></c:pt><c:pt idx="1"><c:v>Q2</c:v></c:pt><c:pt idx="2"><c:v>Q3</c:v></c:pt>
                  </c:strCache></c:strRef></c:cat>
                  <c:val><c:numRef><c:numCache><c:ptCount val="3"/>
                    <c:pt idx="0"><c:v>120</c:v></c:pt><c:pt idx="1"><c:v>150</c:v></c:pt><c:pt idx="2"><c:v>180</c:v></c:pt>
                  </c:numCache></c:numRef></c:val>
                </c:ser>
                <c:ser><c:idx val="1"/>
                  <c:tx><c:strRef><c:strCache><c:ptCount val="1"/><c:pt idx="0"><c:v>South</c:v></c:pt></c:strCache></c:strRef></c:tx>
                  <c:val><c:numRef><c:numCache><c:ptCount val="3"/>
                    <c:pt idx="0"><c:v>90</c:v></c:pt><c:pt idx="1"><c:v>110</c:v></c:pt><c:pt idx="2"><c:v>140</c:v></c:pt>
                  </c:numCache></c:numRef></c:val>
                </c:ser>
              </c:barChart>
            </c:plotArea>
          </c:chart>
        </c:chartSpace>
    """.trimIndent()

    @Test
    fun `a clustered bar chart reads its cached series and categories`() {
        val chart = parse(bar())!!
        assertEquals(ChartModel.Kind.BAR, chart.kind)
        assertEquals("Units per quarter", chart.title)
        assertEquals(listOf("Q1", "Q2", "Q3"), chart.categories)
        assertEquals(listOf("North", "South"), chart.series.map { it.name })
        assertEquals(listOf(120.0, 150.0, 180.0), chart.series[0].values)
        assertEquals(listOf(90.0, 110.0, 140.0), chart.series[1].values)
        assertEquals(180.0, chart.maxValue, 0.0)
    }

    @Test
    fun `a horizontal bar chart is refused rather than drawn standing up`() {
        // barDir "bar" means the bars run left to right. Painting them vertically would show the
        // author a different chart than they made, so the frame stays empty instead.
        assertNull(parse(bar(dir = "bar")))
    }

    @Test
    fun `a sparse cache keeps its holes and its indices`() {
        val xml = bar().replace(
            "<c:pt idx=\"0\"><c:v>120</c:v></c:pt><c:pt idx=\"1\"><c:v>150</c:v></c:pt><c:pt idx=\"2\"><c:v>180</c:v></c:pt>",
            "<c:pt idx=\"2\"><c:v>180</c:v></c:pt>"
        )
        val chart = parse(xml)!!
        assertEquals(listOf(0.0, 0.0, 180.0), chart.series[0].values)
        assertEquals(3, chart.pointCount)
    }

    @Test
    fun `a line chart and a pie chart are both recognised`() {
        val line = parse(
            """
            <c:chartSpace ${c}${a}${r}><c:chart><c:plotArea>
              <c:lineChart><c:marker val="1"/>
                <c:ser><c:tx><c:strRef><c:strCache><c:pt idx="0"><c:v>Revenue</c:v></c:pt></c:strCache></c:strRef></c:tx>
                  <c:cat><c:strRef><c:strCache><c:ptCount val="2"/><c:pt idx="0"><c:v>Jan</c:v></c:pt><c:pt idx="1"><c:v>Feb</c:v></c:pt></c:strCache></c:strRef></c:cat>
                  <c:val><c:numRef><c:numCache><c:ptCount val="2"/><c:pt idx="0"><c:v>12</c:v></c:pt><c:pt idx="1"><c:v>28</c:v></c:pt></c:numCache></c:numRef></c:val>
                </c:ser>
              </c:lineChart>
            </c:plotArea></c:chart></c:chartSpace>
            """.trimIndent()
        )!!
        assertEquals(ChartModel.Kind.LINE, line.kind)
        assertEquals(listOf(12.0, 28.0), line.series.single().values)

        val pie = parse(
            """
            <c:chartSpace ${c}${a}${r}><c:chart><c:plotArea>
              <c:pieChart><c:firstSliceAng val="0"/>
                <c:ser><c:cat><c:strRef><c:strCache><c:ptCount val="2"/>
                  <c:pt idx="0"><c:v>Desktop</c:v></c:pt><c:pt idx="1"><c:v>Mobile</c:v></c:pt></c:strCache></c:strRef></c:cat>
                  <c:val><c:numRef><c:numCache><c:ptCount val="2"/>
                    <c:pt idx="0"><c:v>45</c:v></c:pt><c:pt idx="1"><c:v>40</c:v></c:pt></c:numCache></c:numRef></c:val>
                </c:ser>
              </c:pieChart>
            </c:plotArea></c:chart></c:chartSpace>
            """.trimIndent()
        )!!
        assertEquals(ChartModel.Kind.PIE, pie.kind)
        assertEquals(listOf("Desktop", "Mobile"), pie.categories)
        assertTrue(pie.series.single().values.sum() == 85.0)
    }

    @Test
    fun `a chart part that is not one of the three kinds yields nothing`() {
        val scatter = parse(
            """
            <c:chartSpace ${c}${a}${r}><c:chart><c:plotArea>
              <c:scatterChart><c:ser><c:val><c:numRef><c:numCache><c:pt idx="0"><c:v>1</c:v></c:pt></c:numCache></c:numRef></c:val></c:ser></c:scatterChart>
            </c:plotArea></c:chart></c:chartSpace>
            """.trimIndent()
        )
        assertNull("an unsupported kind must not be drawn as a bar chart", scatter)
    }

    @Test
    fun `a stacked bar chart spans the sum of its column, not its tallest bar`() {
        val xml = bar().replace("<c:barDir val=\"col\"/>", "<c:barDir val=\"col\"/><c:grouping val=\"stacked\"/>")
        val chart = parse(xml)!!
        assertEquals(ChartModel.Grouping.STACKED, chart.grouping)
        assertEquals(320.0, chart.maxValue, 0.0)
        assertEquals(0.0, chart.minValue, 0.0)
        // The second series of the last category sits on top of the first, so it runs 180 -> 320.
        assertEquals(180.0, chart.extents(1, 2).first, 0.0)
        assertEquals(320.0, chart.extents(1, 2).second, 0.0)
        assertEquals(0.0, chart.extents(0, 0).first, 0.0)
        assertEquals(120.0, chart.extents(0, 0).second, 0.0)
    }

    @Test
    fun `a percentage-stacked chart is measured on a zero to hundred axis`() {
        val xml = bar().replace("<c:barDir val=\"col\"/>", "<c:barDir val=\"col\"/><c:grouping val=\"percentStacked\"/>")
        val chart = parse(xml)!!
        assertEquals(ChartModel.Grouping.PERCENT, chart.grouping)
        assertEquals(100.0, chart.maxValue, 0.0)
        assertEquals(0.0, chart.minValue, 0.0)
        // Q1 is 120 of 210, so the first segment ends at 57.1% and the second closes the column.
        assertEquals(57.142857, chart.extents(0, 0).second, 1e-4)
        assertEquals(57.142857, chart.extents(1, 0).first, 1e-4)
        assertEquals(100.0, chart.extents(1, 0).second, 1e-9)
    }

    @Test
    fun `a stacked line chart is refused rather than drawn as separate lines`() {
        val xml = """
            <c:chartSpace ${c}${a}${r}><c:chart><c:plotArea>
              <c:lineChart><c:grouping val="stacked"/>
                <c:ser><c:val><c:numRef><c:numCache><c:ptCount val="2"/>
                  <c:pt idx="0"><c:v>12</c:v></c:pt><c:pt idx="1"><c:v>28</c:v></c:pt></c:numCache></c:numRef></c:val></c:ser>
              </c:lineChart>
            </c:plotArea></c:chart></c:chartSpace>
        """.trimIndent()
        assertNull("a stacked line is a different picture from the lines this painter draws", parse(xml))
    }

    @Test
    fun `a three-dimensional pie is read for the slices it is made of`() {
        val xml = """
            <c:chartSpace ${c}${a}${r}><c:chart><c:plotArea>
              <c:pie3DChart>
                <c:ser><c:cat><c:strRef><c:strCache><c:ptCount val="2"/>
                  <c:pt idx="0"><c:v>Desktop</c:v></c:pt><c:pt idx="1"><c:v>Mobile</c:v></c:pt></c:strCache></c:strRef></c:cat>
                  <c:val><c:numRef><c:numCache><c:ptCount val="2"/>
                    <c:pt idx="0"><c:v>45</c:v></c:pt><c:pt idx="1"><c:v>40</c:v></c:pt></c:numCache></c:numRef></c:val></c:ser>
              </c:pie3DChart>
            </c:plotArea></c:chart></c:chartSpace>
        """.trimIndent()
        val chart = parse(xml)!!
        assertEquals(ChartModel.Kind.PIE, chart.kind)
        assertEquals(listOf("Desktop", "Mobile"), chart.categories)
    }

    @Test
    fun `a combo chart is refused rather than drawn with half its data missing`() {
        val xml = """
            <c:chartSpace ${c}${a}${r}><c:chart><c:plotArea>
              <c:barChart><c:barDir val="col"/>
                <c:ser><c:val><c:numRef><c:numCache><c:pt idx="0"><c:v>10</c:v></c:pt></c:numCache></c:numRef></c:val></c:ser>
              </c:barChart>
              <c:lineChart>
                <c:ser><c:val><c:numRef><c:numCache><c:pt idx="0"><c:v>70</c:v></c:pt></c:numCache></c:numRef></c:val></c:ser>
              </c:lineChart>
            </c:plotArea></c:chart></c:chartSpace>
        """.trimIndent()
        assertNull("only the bars of a bar-and-line chart would have been drawn", parse(xml))
    }

    @Test
    fun `the chart parts PowerPoint itself wrote are read from their own caches`() {
        val bar = fixture("real-chart-bar.xml")
        assertEquals("the bar chart part Office wrote could not be read", ChartModel.Kind.BAR, bar?.kind)
        assertEquals("Units per quarter", bar!!.title)
        assertEquals(listOf("North", "South"), bar.series.map { it.name })
        assertEquals(listOf(120.0, 150.0, 180.0), bar.series[0].values)
        assertEquals(listOf(90.0, 110.0, 140.0), bar.series[1].values)
        assertEquals(listOf("Q1", "Q2", "Q3"), bar.categories)
        assertEquals(ChartModel.Grouping.CLUSTERED, bar.grouping)

        val line = fixture("real-chart-line.xml")
        assertEquals(ChartModel.Kind.LINE, line?.kind)
        assertEquals("Revenue against cost", line!!.title)
        assertEquals(listOf(12.0, 28.0, 21.0), line.series[0].values)
        assertEquals(listOf(9.0, 17.0, 24.0), line.series[1].values)

        val pie = fixture("real-chart-pie.xml")
        assertEquals(ChartModel.Kind.PIE, pie?.kind)
        assertEquals("Traffic by device", pie!!.title)
        assertEquals(listOf("Desktop", "Mobile", "Tablet"), pie.categories)
        assertEquals(listOf(45.0, 40.0, 15.0), pie.series.first().values)
    }

    private fun fixture(name: String): ChartModel.Chart? = javaClass.getResourceAsStream("/fixtures/$name")
        ?.use { ChartModel.parse(OoxmlNode.parse(it)) }
}
