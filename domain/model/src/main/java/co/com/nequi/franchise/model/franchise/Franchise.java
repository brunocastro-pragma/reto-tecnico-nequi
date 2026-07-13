package co.com.nequi.franchise.model.franchise;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class Franchise {

    String id;
    String name;
}
