package auctionsniper;

import auctionsniper.ui.MainWindow;
import auctionsniper.ui.SnipersTableModel;
import auctionsniper.xmpp.XMPPAuctionHouse;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class Main{
    private static final int ARG_HOSTNAME = 0;
    private static final int ARG_USERNAME = 1;
    private static final int ARG_PASSWORD = 2;

    public static void main(String... args) throws Exception {
        Main main = new Main();
        XMPPAuctionHouse auctionHouse =
                XMPPAuctionHouse.connect(
                        args[ARG_HOSTNAME], args[ARG_USERNAME], args[ARG_PASSWORD]);
        main.disconnectWhenUICloses(auctionHouse);
        main.addRequestListenerFor(auctionHouse);
    }

    private final SnipersTableModel snipers = new SnipersTableModel();

    private MainWindow ui;
    private List<Auction> notToBeGCd = new ArrayList<Auction>();

    public Main() throws Exception {
        startUserInterface();
    }

    private void startUserInterface() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                ui = new MainWindow(snipers);
            }
        });
    }

    private void addRequestListenerFor(final AuctionHouse auctionHouse) {
        ui.addUserRequestListener(new UserRequestListener() {
            @Override
            public void joinAuction(String itemId) {
                snipers.addSniper(SniperSnapshot.joining(itemId));

                Auction auction = auctionHouse.auctionFor(itemId);
                notToBeGCd.add(auction);
                auction.addAuctionEventListener(
                        new AuctionSniper(
                                auction,
                                new SwingThreadSniperListener(snipers),
                                itemId));
                auction.join();
            }
        });
    }

    private void disconnectWhenUICloses(final XMPPAuctionHouse xmppAuctionHouse) {
        ui.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                xmppAuctionHouse.disconnect();
            }
        });
    }

    public class SwingThreadSniperListener implements SniperListener {
        private final SniperListener sniperListener;

        public SwingThreadSniperListener(SniperListener sniperListener) {
            this.sniperListener = sniperListener;
        }

        @Override
        public void sniperStateChanged(final SniperSnapshot snapshot) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    sniperListener.sniperStateChanged(snapshot);
                }
            });
        }
    }
}
