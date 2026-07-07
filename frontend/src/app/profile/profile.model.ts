export interface Profile {
  freelancerId: string;
  skills: string[];
  expectedDailyRateAmount: number;
  expectedDailyRateCurrency: string;
}

export interface UpdateProfileRequest {
  skills: string[];
  expectedDailyRateAmount: number;
  expectedDailyRateCurrency: string;
}
