package ru.qatools.gawain

import groovy.transform.CompileStatic
import org.junit.Test

import static org.hamcrest.CoreMatchers.equalTo
import static org.junit.Assert.assertThat
import static ru.qatools.gawain.Opts.opts

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class OptsTest {

    @Test
    public void testOptsWithValues() throws Exception {
        def o = opts(bbb: 20).set(aaa: 10)
        assertThat(o['aaa'] as int, equalTo(10))
        assertThat(o['bbb'] as int, equalTo(20))

        o = new Opts(aaa: 20, maxQueueSize: 110)
        assertThat(o['aaa'] as int, equalTo(20))
        assertThat(o.maxQueueSize as int, equalTo(110))

        o = new Opts(['aaa', 20, 'bbb', 10])
        assertThat(o['aaa'] as int, equalTo(20))
        assertThat(o['bbb'] as int, equalTo(10))

        o = opts(['aaa', 50])
        assertThat(o['aaa'] as int, equalTo(50))
    }

    @Test(expected = IllegalAccessException)
    public void testInvalidList() throws Exception {
        new Opts(['aaa', 20, 'bbb'])
    }
}
