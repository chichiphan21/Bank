package org.dacs.Common;


import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ServerAddress {
    private String address;
    private Integer port;
}
