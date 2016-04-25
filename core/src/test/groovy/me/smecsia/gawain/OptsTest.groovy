package me.smecsia.gawain

import groovy.transform.CompileStatic
import org.junit.Test

import static me.smecsia.gawain.Opts.opts
import static org.hamcrest.CoreMatchers.equalTo
import static org.junit.Assert.assertThat

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

        o = opts('aaa', 33)
        assertThat(o['aaa'] as int, equalTo(33))
    }

    @Test(expected = IllegalAccessException)
    public void testInvalidList() throws Exception {
        new Opts(['aaa', 20, 'bbb'])
    }

    @Test
    public void testDeepClone() throws Exception {
        def opts = new Opts(aaa: [aaa: 10], global: true)
        def copy = opts.clone() as Opts
        opts['aaa']['aaa'] = 20
        assertThat(opts['aaa']['aaa'] as int, equalTo(20))
        assertThat(opts.global, equalTo(true))
        assertThat(copy['aaa']['aaa'] as int, equalTo(10))
        assertThat(copy.global, equalTo(true))
    }
}
