package strategies;

import jade.core.AID;
import simulation.World;
import types.Contract;

public class SimpleTechnicianStrategy extends TechnicianStrategy {
    @Override
    public boolean lookForContracts() {
        int day = World.get().getDay();
        Contract current = technician.getCurrentContract();
        return current.end - day <= 3;  // run 3 days before contract expires
    }

    @Override
    public Contract renewalContract() {
        int day = World.get().getDay();
        Contract cp = technician.getCurrentContract();

        AID company = cp.company;
        String station = technician.getHomeStation().getLocalName();
        double salary = cp.salary * Math.pow(1.005, cp.numDays());
        double percentage = cp.percentage;
        int start = Math.max(cp.end + 1, day);
        int end = start + Math.min(cp.numDays(), 10);
        return new Contract(company, technician.getAID(), station, salary, percentage, start, end);
    }
}
