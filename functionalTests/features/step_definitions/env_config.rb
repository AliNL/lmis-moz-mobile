module EnvConfig

    STRESS_TEST = false

    def self.getConfig()

        dev_env=true

        if dev_env
            {
                username:"superuser",
                password:"password1",
            }
        else
            {
                username:"test_user",
                password:"testuser",
            }
        end
    end
end
