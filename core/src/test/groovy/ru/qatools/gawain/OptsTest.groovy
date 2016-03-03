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
        def opts = opts(bbb: 20).set(aaa: 10)
        assertThat(opts['aaa'] as int, equalTo(10))
        assertThat(opts['bbb'] as int, equalTo(20))

        opts = new Opts(aaa: 20, maxQueueSize: 110)
        assertThat(opts['aaa'] as int, equalTo(20))
        assertThat(opts.maxQueueSize as int, equalTo(110))
    }
}
