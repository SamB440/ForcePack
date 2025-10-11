package com.convallyria.forcepack.api.resourcepack;

public final class PackFormatResolver {

    public static double getPackFormat(int protocol) {
        switch (protocol) {
            case 773: // 1.21.9-1.21.10
                return 69;
            case 772: // 1.21.7-1.21.8
                return 64;
            case 771: // 1.21.6
                return 63;
            case 770: // 1.21.5
                return 55;
            case 769: // 1.21.4
                return 46;
            case 768: // 1.21.2-1.21.3
                return 42;
            case 767: // 1.21-1.21.1
                return 34;
            case 766: // 1.20.5-1.20.6
                return 32;
            case 765: // 1.20.3-1.20.4
                return 22;
            case 764: // 1.20.2
                return 18;
            case 763:
                return 15;
            case 762:
                return 13;
            case 761:
                return 12;
            case 760:
            case 759:
                return 9;
            case 758:
            case 757:
                return 8;
            case 756:
            case 755:
                return 7;
            case 754:
            case 753:
            case 751:
                return 6;
            case 736:
            case 735:
            case 578:
            case 575:
            case 573:
                return 5;
            case 498:
            case 490:
            case 485:
            case 480:
            case 477:
            case 404:
            case 401:
            case 393:
                return 4;
            case 340:
            case 338:
            case 335:
            case 316:
            case 315:
                return 3;
            case 210:
            case 110:
            case 109:
            case 108:
            case 107:
                return 2;
            default:
                return 1;
        }
    }
}
