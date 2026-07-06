export type MissionStatus = 'OPEN' | 'CLOSED';

export interface Mission {
  id: string;
  title: string;
  clientName: string;
  requiredSkills: string[];
  dailyRateAmount: number;
  startDate: string;
  status: MissionStatus;
}

export interface PublishMissionRequest {
  title: string;
  clientName: string;
  requiredSkills: string[];
  dailyRateAmount: number;
  startDate: string;
}
