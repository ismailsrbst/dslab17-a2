package dslab.mailbox;

import static dslab.StringMatches.matchesPattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dslab.ComponentFactory;
import dslab.Constants;
import dslab.JunitSocketClient;
import dslab.Sockets;
import dslab.TestBase;
import dslab.util.Config;

public class MailboxStartsecureTest extends TestBase {

    private static final Log LOG = LogFactory.getLog(MailboxServerProtocolTest.class);

    private int dmapServerPort;
    private int dmepServerPort;

    @Before
    public void setUp() throws Exception {
        String componentId = "mailbox-earth-planet";

        IMailboxServer component = ComponentFactory.createMailboxServer(componentId, in, out);
        dmapServerPort = new Config(componentId).getInt("dmap.tcp.port");
        dmepServerPort = new Config(componentId).getInt("dmep.tcp.port");

        new Thread(component).start();

        LOG.info("Waiting for server sockets to appear");
        Sockets.waitForSocket("localhost", dmapServerPort, Constants.COMPONENT_STARTUP_WAIT);
        Sockets.waitForSocket("localhost", dmepServerPort, Constants.COMPONENT_STARTUP_WAIT);
    }

    @After
    public void tearDown() throws Exception {
        in.addLine("shutdown"); // send "shutdown" command to command line
        Thread.sleep(Constants.COMPONENT_TEARDOWN_WAIT);
    }

    @Test(timeout = 15000)
    public void sendStartsecure() throws Exception {

        // a challenge, aes secret and iv param encrypted with the server's RSA key
        String testChallenge = "FIuFybsN7XAn2d3l0AEHj4xm+Q3tLbgqDcElXGUQY67mQqT8MZ3lq6PRWeiXHA9fjKtHimm8qYQagfFb9Kb0" +
                "2/Gk4sNL703giQzDnugQ+JORI0lpOffqgF1tfD+t6N9Flm2ChGkP8qSEJeoBEnl4RpKPvK6r1YorcWCk+IwhUMchf5w5zVwPDca" +
                "KyufXOJbIDvIHylYTo6/rSwGLDkdM+zHn0EzcGjcNPTc8gUiQWa1IRnWsuHjOXt969qZq+R4kbCZl+W7Mtr0oBmluuwouLzJhiR" +
                "ESxC6F9xNZxNt9Fzt5uRA5tVjKmT2tDAHr5ZG3SPHgGS66x0yZgZw3OvkoOkkVsE4Mc4vpdFukP1F2ddaVUOHEgkh1eYiejxW7E" +
                "v4sOpEd/K4S0nihBzj5CIsWD1FbalZ5+odtyCumVF/QNuPouxocaB6iQgPOMYKpaNRUPbSybAD9+/X92r9VI2C0D4hYbLhhovoZ" +
                "sm7w2DqvQKM1F9YMzL/V5/Af9MeOvWY120pfjgLKRrEBR7sjzY4PLK44LTBfCzPaWlW3k9lm2LAit6OnIgY19tx0Ia/8jHihuxb" +
                "W6YLJ1ApbxZeP4W04wwiuLhnjFvUqUVF5uX/u0ZqKVC0dACEvpqSE2oxI2PeK1ecRHfW6QgBSeMg+qPaOnBTDOipJMEFHfP11Qw" +
                "t+a04=";

        try (JunitSocketClient client = new JunitSocketClient(dmapServerPort, err)) {
            // protocol check
            client.verify("ok DMAP2.0");

            // check that mailbox returns its component id
            client.sendAndVerify("startsecure", "ok mailbox-earth-planet");

            // send the challenge + aes init
            client.send(testChallenge);

            // response should be "ok <challenge>" (which is AES encrypted and base64 encoded)
            // specifically it should be g9UJxNFULO+H0otZoH5AVXoHv9TxJUEcbY/ScWoWMvcJYLz2lYBaZ16OtqEKtVk=
            err.checkThat("Expected server response to be Base64 encoded", client.listen(),
                    matchesPattern("^(?:[a-zA-Z0-9+/]{4})*(?:[a-zA-Z0-9+/]{2}==|[a-zA-Z0-9+/]{3}=)?$"));

            // send encrypted "ok" (with the aes cipher) in
            client.send("g9U=");
        }
    }


}
